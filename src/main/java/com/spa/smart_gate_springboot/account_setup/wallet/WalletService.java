package com.spa.smart_gate_springboot.account_setup.wallet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cash-wallet ledger service. Every mutation is:
 *   1. idempotent on {@code externalRef} (returns the existing ledger row on a duplicate);
 *   2. serialized via a pessimistic row lock ({@link WalletRepository#findByWalletCodeForUpdate});
 *   3. recorded as a signed {@link WalletTransaction} with {@code balanceAfter}.
 *
 * Mirrors the proven nineyard-capital wallet pattern, keyed on {@code walletCode} instead of an owner id.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    public static final String TOP_WALLET_CODE = "TOP_PLATFORM";

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public static String walletCodeForReseller(UUID resellerId) {
        return "RS_" + resellerId;
    }

    @Transactional(readOnly = true)
    public Wallet getByCode(String walletCode) {
        return walletRepository.findByWalletCode(walletCode)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletCode));
    }

    /** Returns the existing wallet for {@code walletCode}, creating it (balance 0) if absent. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Wallet getOrCreate(WalletOwnerType ownerType, UUID ownerId, String walletCode) {
        return walletRepository.findByWalletCode(walletCode).orElseGet(() -> {
            Wallet wallet = Wallet.builder()
                    .ownerType(ownerType)
                    .ownerId(ownerId)
                    .walletCode(walletCode)
                    .balance(BigDecimal.ZERO)
                    .lockedBalance(BigDecimal.ZERO)
                    .currency("KES")
                    .status(WalletStatus.ACTIVE)
                    .build();
            try {
                return walletRepository.saveAndFlush(wallet);
            } catch (DataIntegrityViolationException e) {
                // Concurrent creator won the race — load theirs.
                return walletRepository.findByWalletCode(walletCode)
                        .orElseThrow(() -> e);
            }
        });
    }

    public Wallet getOrCreateReseller(UUID resellerId) {
        return getOrCreate(WalletOwnerType.RESELLER, resellerId, walletCodeForReseller(resellerId));
    }

    public Wallet getOrCreateTop() {
        return getOrCreate(WalletOwnerType.TOP, null, TOP_WALLET_CODE);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletTransaction credit(String walletCode, BigDecimal amount, WalletTxType txType,
                                    String externalRef, String narration, UUID createdBy) {
        WalletTransaction existing = findExisting(externalRef);
        if (existing != null) {
            log.warn("Duplicate credit skipped — externalRef {} already processed", externalRef);
            return existing;
        }

        Wallet wallet = lockWallet(walletCode);
        wallet.setBalance(safe(wallet.getBalance()).add(amount));
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .walletCode(walletCode)
                .txType(txType)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .externalRef(externalRef)
                .narration(narration)
                .createdBy(createdBy)
                .build();
        WalletTransaction saved = persistLedger(tx, externalRef);
        log.info("Credited {} to wallet {} ({}), new balance {}", amount, walletCode, txType, wallet.getBalance());
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletTransaction debit(String walletCode, BigDecimal amount, WalletTxType txType,
                                   String externalRef, String narration, UUID createdBy) {
        WalletTransaction existing = findExisting(externalRef);
        if (existing != null) {
            log.warn("Duplicate debit skipped — externalRef {} already processed", externalRef);
            return existing;
        }

        Wallet wallet = lockWallet(walletCode);
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient wallet balance. Available: KES "
                    + wallet.getAvailableBalance() + ", requested KES " + amount);
        }

        wallet.setBalance(safe(wallet.getBalance()).subtract(amount));
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .walletCode(walletCode)
                .txType(txType)
                .amount(amount.negate())
                .balanceAfter(wallet.getBalance())
                .externalRef(externalRef)
                .narration(narration)
                .createdBy(createdBy)
                .build();
        WalletTransaction saved = persistLedger(tx, externalRef);
        log.info("Debited {} from wallet {} ({}), new balance {}", amount, walletCode, txType, wallet.getBalance());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<WalletTransaction> getTransactions(String walletCode, Pageable pageable) {
        return walletTransactionRepository.findByWalletCodeOrderByCreatedAtDesc(walletCode, pageable);
    }

    /** True if a ledger row for this externalRef already exists (operation already processed). */
    @Transactional(readOnly = true)
    public boolean isProcessed(String externalRef) {
        return externalRef != null && walletTransactionRepository.existsByExternalRef(externalRef);
    }

    /**
     * Debits both withdrawal legs (payout + charge) atomically in one transaction. Because this is
     * invoked cross-bean, the proxy is active and both debits commit or roll back together — if the
     * charge debit fails, the payout debit is rolled back (no half-debit can strand funds).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void debitWithdrawalLegs(String walletCode, BigDecimal payout, BigDecimal charge,
                                    String ref, String phoneNumber, UUID createdBy) {
        debit(walletCode, payout, WalletTxType.WITHDRAWAL, ref,
                "Withdrawal to " + phoneNumber, createdBy);
        if (charge != null && charge.signum() > 0) {
            debit(walletCode, charge, WalletTxType.MPESA_CHARGE, ref + "_CHG",
                    "M-Pesa withdrawal charge", createdBy);
        }
    }

    /**
     * Credits both withdrawal legs back atomically (compensation). {@code refSuffix} distinguishes the
     * auto-reversal ("_REV") from an admin reversal ("_ADMINREV") so the two never collide on externalRef.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void creditWithdrawalReversalLegs(String walletCode, BigDecimal payout, BigDecimal charge,
                                             String ref, String refSuffix, String narration, UUID createdBy) {
        credit(walletCode, payout, WalletTxType.WITHDRAWAL_REVERSAL, ref + refSuffix, narration, createdBy);
        if (charge != null && charge.signum() > 0) {
            credit(walletCode, charge, WalletTxType.MPESA_CHARGE_REVERSAL, ref + "_CHG" + refSuffix,
                    narration + " (charge)", createdBy);
        }
    }

    // ── internals ──────────────────────────────────────────────────────────────

    private WalletTransaction findExisting(String externalRef) {
        if (externalRef == null) return null;
        if (walletTransactionRepository.existsByExternalRef(externalRef)) {
            return walletTransactionRepository.findByExternalRef(externalRef).orElse(null);
        }
        return null;
    }

    private Wallet lockWallet(String walletCode) {
        return walletRepository.findByWalletCodeForUpdate(walletCode)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletCode));
    }

    /**
     * Persists the ledger row. The pre-lock {@code existsByExternalRef} check can race two concurrent
     * duplicates past it; the unique index on {@code external_ref} is the real backstop — on collision
     * we return the row the winner inserted instead of surfacing a 500.
     */
    private WalletTransaction persistLedger(WalletTransaction tx, String externalRef) {
        try {
            return walletTransactionRepository.saveAndFlush(tx);
        } catch (DataIntegrityViolationException e) {
            if (externalRef != null) {
                WalletTransaction won = walletTransactionRepository.findByExternalRef(externalRef).orElse(null);
                if (won != null) {
                    log.warn("Ledger insert lost externalRef race for {} — returning winning row", externalRef);
                    return won;
                }
            }
            throw e;
        }
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
