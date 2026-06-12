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

    @Column(name = "wallet_code", nullable = false)
    private String walletCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false)
    private WalletTxType txType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "external_ref", length = 120)
    private String externalRef;

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
