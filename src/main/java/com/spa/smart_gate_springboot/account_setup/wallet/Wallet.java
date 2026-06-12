package com.spa.smart_gate_springboot.account_setup.wallet;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A real cash wallet (KES) held on behalf of a RESELLER or the platform owner (TOP).
 * Keyed by {@code walletCode}: resellers use "RS_&lt;rsId&gt;", TOP uses the singleton "TOP_PLATFORM".
 *
 * <p>This is NEW cash money, layered on top of the existing SMS-units accounting
 * ({@code Reseller.rsAllocatableUnit}/{@code rsMsgBal}, {@code Account.accMsgBal}).
 *
 * <p>{@code ownerType} and {@code status} are persisted as ORDINAL (no CHECK constraint) on purpose —
 * see the {@code Account.accStatus} precedent and the project's ddl-auto:update CHECK-constraint rule.
 */
@Entity
@Table(name = "cash_wallet", schema = "msg",
        uniqueConstraints = @UniqueConstraint(name = "uq_cash_wallet_code", columnNames = "wallet_code"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue
    @Column(name = "wallet_id")
    private UUID walletId;

    @Column(name = "owner_type", nullable = false)
    private WalletOwnerType ownerType;

    /** Reseller id for RESELLER wallets; null for the TOP singleton. */
    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "wallet_code", nullable = false, unique = true)
    private String walletCode;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /** Reserved funds not spendable (reserved for future use; available = balance − lockedBalance). */
    @Column(name = "locked_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "KES";

    @Column(nullable = false)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;

    /** Optimistic-lock version (defence-in-depth alongside the pessimistic row lock on mutation). */
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getAvailableBalance() {
        BigDecimal bal = balance == null ? BigDecimal.ZERO : balance;
        BigDecimal locked = lockedBalance == null ? BigDecimal.ZERO : lockedBalance;
        return bal.subtract(locked);
    }
}
