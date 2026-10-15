package com.spa.smart_gate_springboot.account_setup.reseller;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
//@EntityListeners(AvailableBalanceListener.class)
@Table(name = "reseller", schema = "js_core")
public class Reseller {
    @Id
    @GeneratedValue
    private UUID rsId;

    @Column(nullable = false)
    @NotNull
    private BigDecimal rsSmsUnitPrice;

    @Column(nullable = false, unique = true)
    private String rsCompanyName;

    @Column(nullable = false, unique = true)
    private String rsEmail;

    @Column(nullable = false)
    private String rsPhoneNumber;

    @Column(nullable = false)
    private String rsContactPerson;

    private String raCity;

    private String rsLogo;

    private String rsIcon;
    private String raState;
    private String raPostalCode;
    @Builder.Default
    private String raCountry = "KENYA";
    private String raWebsite;
    private String rsDomain;
    @Column(nullable = false)
    private String vatNumber;

    @Column(nullable = false)
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    @Column(nullable = false)
    private boolean isActive;

    @Enumerated(EnumType.STRING)
    private  BusinessType rsBusinessType;
    private UUID rsCreatedBy;
    private BigDecimal rsMsgBal;

    private String rsStatus;
    private String rsKraPin;
    /**
     * Allocatable-unit balance at or below which the reseller gets a low-balance alert (email + SMS).
     * Null falls back to the platform default {@code sms.low-balance.threshold}; 0 turns alerts off.
     * Stored as text because the column already existed as varchar ({@code ddl-auto: update} never
     * changes a column's type), so {@code ResellerService.saveReseller} only accepts a whole number
     * and the alert query ignores anything that is not one.
     */
    private String rsReorderLevel;
    private BigDecimal rsAllocatableUnit;

    /** Last low-balance alert sent to the reseller; throttles alerts like {@code acc_last_low_bal_alert}. */
    private LocalDateTime rsLastLowBalAlert;

    /**
     * Available cash-wallet balance (KES) for this reseller — populated on listing from the separate
     * {@code cash_wallet} table (keyed RS_&lt;rsId&gt;). Not persisted on the reseller row.
     */
    @Transient
    private BigDecimal rsWalletBalance;

    // Deletion tracking fields
    private LocalDateTime rsDeletedDate;
    private String rsDeletedByName;
    private String rsDeletedReason;
    private UUID rsDeletedBy;


}

