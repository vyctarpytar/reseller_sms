package com.spa.smart_gate_springboot.account_setup.credit;


import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
@Table(schema = "js_core")
@Entity(name = "jsc_accounts_sms_payment")
public class Credit {

    @Id
    @GeneratedValue
    private UUID smsId;
    private UUID smsResellerId;
    private String smsResellerName;
    private String smsAccountName;
    private UUID smsAccId;

    @NotNull(message = "Field Cannot be null")
    @Min(0)
    private BigDecimal smsPayAmount;
    private Long smsLoaded;
    @Min(0)
    private BigDecimal smsRate;
    private BigDecimal smsPrevBal;
    private BigDecimal smsNewBal;
    private BigDecimal smsNewResellerUnits;

    @Column(nullable = false, updatable = false)
    private UUID smsCreatedBy;
    private String  smsCreatedByName;
    private LocalDateTime smsCreatedDate;

    // smsApproved* fields removed with the manager-approval flow. The DB columns are left in place
    // (ddl-auto:update never drops columns) but are no longer mapped or written.

    @Enumerated(EnumType.STRING)
    private CrStatus crStatus;

    private String smsPaymentRef;
     private UUID smsPaymentId;
     private String smsLoadingMethod;


}


