package com.spa.smart_gate_springboot.account_setup.wallet;

import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.payment.mpesa.b2c.B2cPurpose;
import com.spa.smart_gate_springboot.payment.mpesa.b2c.B2cTransaction;
import com.spa.smart_gate_springboot.payment.mpesa.b2c.B2cTransactionRepository;
import com.spa.smart_gate_springboot.payment.mpesa.b2c.B2cTransactionStatus;
import com.spa.smart_gate_springboot.payment.mpesa.charge.MpesaB2cCharge;
import com.spa.smart_gate_springboot.payment.mpesa.charge.MpesaB2cChargeRepository;
import com.spa.smart_gate_springboot.payment.mpesa.gateway.WaretechMpesaService;
import com.spa.smart_gate_springboot.payment.mpesa.gateway.dto.GatewayB2cResponse;
import com.spa.smart_gate_springboot.account_setup.wallet.dto.WithdrawDto;
import com.spa.smart_gate_springboot.messaging.send_message.SystemSmsService;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.user.UserService;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import com.spa.smart_gate_springboot.utils.UniqueCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cash-out via M-Pesa B2C for RESELLER and TOP wallets. The recipient bears the M-Pesa charge:
 *   gross requested → charge looked up by band → payout = gross − charge → wallet debited gross
 *   (two ledger lines: WITHDRAWAL=payout, MPESA_CHARGE=charge) → B2C sends payout.
 * On B2C rejection/failure at initiation, BOTH ledger lines are reversed in the same transaction.
 * A later FAILED status (poller) keeps the wallet debited until an admin runs {@link #reverseWithdrawal}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletWithdrawalService {

    private final WalletService walletService;
    private final MpesaB2cChargeRepository chargeRepository;
    private final WaretechMpesaService gatewayService;
    private final B2cTransactionRepository b2cRepository;
    private final UserService userService;
    private final SystemSmsService systemSmsService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final BigDecimal MIN_WITHDRAWAL = new BigDecimal("10");

    public BigDecimal chargeFor(BigDecimal gross) {
        return chargeRepository.findByAmount(gross)
                .map(MpesaB2cCharge::getCharge)
                // Amounts above the top band (or below the lowest) fall through — clamp to the
                // highest defined band's charge rather than silently charging zero.
                .orElseGet(() -> chargeRepository.findAllByOrderByMinAmountAsc().stream()
                        .reduce((first, second) -> second)  // last == highest band
                        .map(MpesaB2cCharge::getCharge)
                        .orElse(BigDecimal.ZERO));
    }

    /** Resolve which wallet a user withdraws from. */
    public String resolveWalletCode(User user) {
        if (user.getLayer() == Layers.TOP) return WalletService.TOP_WALLET_CODE;
        if (user.getLayer() == Layers.RESELLER && user.getUsrResellerId() != null) {
            return WalletService.walletCodeForReseller(user.getUsrResellerId());
        }
        throw new IllegalStateException("User cannot withdraw — not a TOP or RESELLER wallet owner");
    }

    /**
     * Step 1 of withdrawal: generate an OTP, SMS it to the requesting user's phone, store its hash on
     * the user, and echo back the request with a correlation id. Mirrors the legacy NdovuPay 2-step flow.
     */
    public StandardJsonResponse initiateWithdrawOtp(User user, WithdrawDto dto) {
        StandardJsonResponse response = new StandardJsonResponse();

        if (dto.getWithDrawAmount() == null || dto.getWithDrawAmount().compareTo(MIN_WITHDRAWAL) < 0) {
            response.setSuccess(false);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage("message", "Minimum withdrawal is KES " + MIN_WITHDRAWAL, response);
            return response;
        }

        String code = new UniqueCodeGenerator().getUniqueCode();
        String msg = "Use code " + code + " to withdraw KES " + dto.getWithDrawAmount()
                + " to " + dto.getWithDrawPhoneNumber();
        final String msisdn = user.getPhoneNumber();

        // Fire-and-forget SMS — never block/fail the request on the SMS gateway.
        systemSmsService.sendSms(msisdn, msg);

        user.setUsrPhoneWithdrawOtp(passwordEncoder.encode(code));
        user.setUsrOtpStatus("WITHDRAW_OTP_SENT");
        userService.save(user);

        dto.setWithDrawLogId(UUID.randomUUID());
        dto.setWithDrawCode(null); // never echo the code
        response.setMessage("message", "Withdraw OTP sent to " + msisdn, response);
        response.setData("result", dto, response);
        return response;
    }

    /**
     * Step 2: verify the OTP against the user's stored hash, then run the B2C withdrawal using the
     * amount + phone the client re-supplies.
     */
    public StandardJsonResponse finalizeWithdrawOtp(User user, WithdrawDto dto) {
        StandardJsonResponse response = new StandardJsonResponse();

        boolean matched = dto.getWithDrawCode() != null
                && user.getUsrPhoneWithdrawOtp() != null
                && passwordEncoder.matches(dto.getWithDrawCode(), user.getUsrPhoneWithdrawOtp());
        if (!matched) {
            response.setSuccess(false);
            response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
            response.setMessage("message", "Invalid OTP", response);
            return response;
        }

        // Invalidate the OTP so it cannot be replayed.
        user.setUsrPhoneWithdrawOtp(null);
        user.setUsrOtpStatus("WITHDRAW_OTP_USED");
        userService.save(user);

        return withdraw(user, dto.getWithDrawAmount(), dto.getWithDrawPhoneNumber());
    }

    /**
     * Saga-style withdrawal (NOT one DB transaction — the atomic unit is each WalletService call):
     *   1. debit gross atomically (payout + charge legs, via {@link WalletService#debitWithdrawalLegs});
     *   2. call B2C;
     *   3. on failure, compensate atomically and mark the B2C row reversed.
     * Compensation is wrapped so a compensation failure is logged with the B2C id for manual recovery
     * rather than silently leaving the wallet debited.
     */
    public StandardJsonResponse withdraw(User user, BigDecimal gross, String phoneNumber) {
        StandardJsonResponse response = new StandardJsonResponse();

        if (gross == null || gross.compareTo(MIN_WITHDRAWAL) < 0) {
            response.setSuccess(false);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage("message", "Minimum withdrawal is KES " + MIN_WITHDRAWAL, response);
            return response;
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            response.setSuccess(false);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage("message", "Recipient phone number is required", response);
            return response;
        }

        String walletCode;
        B2cPurpose purpose;
        if (user.getLayer() == Layers.TOP) {
            walletCode = WalletService.TOP_WALLET_CODE;
            purpose = B2cPurpose.TOP_WITHDRAWAL;
        } else if (user.getLayer() == Layers.RESELLER && user.getUsrResellerId() != null) {
            walletCode = WalletService.walletCodeForReseller(user.getUsrResellerId());
            purpose = B2cPurpose.RESELLER_WITHDRAWAL;
        } else {
            response.setSuccess(false);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setMessage("message", "Only TOP or RESELLER can withdraw", response);
            return response;
        }

        BigDecimal charge = chargeFor(gross);
        BigDecimal payout = gross.subtract(charge);
        if (payout.signum() <= 0) {
            response.setSuccess(false);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage("message", "Withdrawal amount must exceed the M-Pesa charge of KES " + charge, response);
            return response;
        }

        Wallet wallet = walletService.getByCode(walletCode);
        if (wallet.getAvailableBalance().compareTo(gross) < 0) {
            response.setSuccess(false);
            response.setStatus(HttpStatus.EXPECTATION_FAILED.value());
            response.setMessage("message", "Insufficient balance. Available KES "
                    + wallet.getAvailableBalance() + ", requested KES " + gross, response);
            return response;
        }

        String ref = "WD_" + walletCode + "_" + UUID.randomUUID();

        // Debit the full gross as two ledger lines (payout + charge) — atomic (both or neither).
        walletService.debitWithdrawalLegs(walletCode, payout, charge, ref, phoneNumber, user.getUsrId());

        B2cTransaction b2c = B2cTransaction.builder()
                .walletCode(walletCode)
                .purpose(purpose)
                .phoneNumber(phoneNumber)
                .amount(payout)
                .charge(charge)
                .status(B2cTransactionStatus.PROCESSING)
                .externalRef(ref)
                .createdBy(user.getUsrId())
                .createdByEmail(user.getEmail())
                .build();

        try {
            GatewayB2cResponse resp = gatewayService.initiateB2c(phoneNumber, payout,
                    "Wallet withdrawal " + walletCode);
            if (resp != null && resp.isAccepted()) {
                b2c.setConversationId(resp.getConversationId());
                b2c.setOriginatorConversationId(resp.getOriginatorConversationId());
                b2c.setResponseCode(resp.getResponseCode());
                b2c.setResponseDescription(resp.getResponseDescription());
                b2cRepository.save(b2c);
                response.setMessage("message", "Withdrawal of KES " + payout + " initiated (charge KES "
                        + charge + "). Funds will arrive shortly.", response);
                response.setData("result", b2c, response);
                return response;
            }
            throw new RuntimeException(resp != null ? resp.getResponseDescription() : "no gateway response");
        } catch (Exception e) {
            // Compensate BOTH legs atomically and mark the row reversed so admin reversal can't double-credit.
            boolean compensated = false;
            try {
                walletService.creditWithdrawalReversalLegs(walletCode, payout, charge, ref, "_REV",
                        "Withdrawal reversal — B2C failed: " + e.getMessage(), user.getUsrId());
                compensated = true;
            } catch (Exception comp) {
                // The debit is committed but compensation failed — surface loudly for manual recovery.
                log.error("CRITICAL: withdrawal compensation FAILED for ref {} wallet {} (gross {} stays debited): {}",
                        ref, walletCode, gross, comp.getMessage(), comp);
            }
            b2c.setStatus(B2cTransactionStatus.FAILED);
            b2c.setReversed(compensated);
            b2c.setFailureReason(e.getMessage());
            b2cRepository.save(b2c);

            response.setSuccess(false);
            response.setStatus(HttpStatus.BAD_GATEWAY.value());
            response.setMessage("message", compensated
                    ? "Withdrawal failed and was reversed: " + e.getMessage()
                    : "Withdrawal failed; reversal pending — contact support (ref " + ref + ")", response);
            return response;
        }
    }

    /**
     * Admin reversal for a B2C payout that terminally FAILED at the gateway AFTER acceptance (the poller
     * leaves the wallet debited). Refuses any row already reversed (e.g. auto-reversed on initiation
     * failure) so the wallet can never be credited back twice.
     */
    @Transactional
    public StandardJsonResponse reverseWithdrawal(UUID b2cId, User admin) {
        StandardJsonResponse response = new StandardJsonResponse();
        B2cTransaction b2c = b2cRepository.findById(b2cId)
                .orElseThrow(() -> new RuntimeException("B2C transaction not found: " + b2cId));

        if (b2c.getStatus() != B2cTransactionStatus.FAILED) {
            response.setSuccess(false);
            response.setStatus(HttpStatus.CONFLICT.value());
            response.setMessage("message", "Only FAILED payouts can be reversed (status is " + b2c.getStatus() + ")", response);
            return response;
        }
        if (b2c.isReversed()) {
            response.setSuccess(false);
            response.setStatus(HttpStatus.CONFLICT.value());
            response.setMessage("message", "This withdrawal was already reversed — wallet already credited back", response);
            return response;
        }

        walletService.creditWithdrawalReversalLegs(b2c.getWalletCode(), b2c.getAmount(), b2c.getCharge(),
                b2c.getExternalRef(), "_ADMINREV", "Admin reversal of failed withdrawal", admin.getUsrId());
        b2c.setReversed(true);
        b2cRepository.save(b2c);

        response.setMessage("message", "Withdrawal reversed — KES " + b2c.getAmount() + " credited back", response);
        response.setData("result", b2c, response);
        return response;
    }
}
