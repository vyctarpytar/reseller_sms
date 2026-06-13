package com.spa.smart_gate_springboot.account_setup.wallet;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable ledger row for a single cash-wallet movement. {@code amount} is signed
 * (positive credit, negative debit) and {@code balanceAfter} captures the wallet balance
 * immediately after this row, for audit/reconciliation.
 *
 * <p>{@code externalRef} is the idempotency / correlation key (M-Pesa transId, withdrawal ref,
 * unit-purchase key). The unique index on it is the real backstop against double-processing.
 */
@Entity
@Table(name = "cash_wallet_transaction", schema = "msg",
        indexes = {
                @Index(name = "idx_cash_wallet_tx_code", columnList = "wallet_code"),
                @Index(name = "idx_cash_wallet_tx_ref", columnList = "external_ref", unique = true)
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue
    @Column(name = "tx_id")
    private UUID txId;

    /** The cash wallet this row moved. Null for UNIT legs and ACCOUNT legs, which have no cash wallet. */
    @Column(name = "wallet_code")
    private String walletCode;

    /** KSH (cash) or UNIT (SMS-unit) movement — the statement value-type filter. */
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type")
    @Builder.Default
    private WalletValueType valueType = WalletValueType.KSH;

    /** Whose ledger line this is: TOP / RESELLER / ACCOUNT. */
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type")
    private WalletOwnerType ownerType;

    /** Owner id: reseller id or account id; null for the TOP singleton. */
    @Column(name = "owner_id")
    private UUID ownerId;

    /** Reseller context of the event (set on every leg, incl. the TOP counterparty) — powers the reseller filter. */
    @Column(name = "reseller_id")
    private UUID resellerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false)
    private WalletTxType txType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** Running balance after this row, in the row's value-type. Null for legs whose balance isn't tracked (e.g. TOP units). */
    @Column(name = "balance_after", precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "external_ref", length = 120)
    private String externalRef;

    /**
     * The account whose purchase triggered this movement, when applicable (e.g. an account buying
     * SMS units credits the owning reseller's wallet). Null for movements with no account dimension
     * (reseller→TOP unit sales, withdrawals, adjustments). Drives the account filter on the statement.
     */
    @Column(name = "account_id")
    private UUID accountId;

    private String narration;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
