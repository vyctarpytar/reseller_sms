package com.spa.smart_gate_springboot.account_setup.account;

import com.spa.smart_gate_springboot.account_setup.shortsetup.MsgShortcodeSetup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
@Builder
@Table(schema = "js_core")
@Entity(name = "jsc_accounts")
public class Account {
    @Id
    @GeneratedValue
    @Column(name = "acc_id")
    private UUID accId;
    private UUID accResellerId;
    @Column(nullable = false)
    @NotNull(message = "accName cannot be null")
    private String accName;
    private String accWebsite;
    private String accOfficeMobile;
    private String accOfficeTel;
    private String accStreet;
    private String accPostalAddress;
    private String accPostalCode;
    private String accCity;
    @Builder.Default
    private String accCountry = "KENYA";
    private String accIndustry;
    private String accComment;
    private UUID accAssignedTo;
    @Column(nullable = false, updatable = false)
    private UUID accCreatedBy;
    private LocalDateTime accCreatedDate;
    private String accCode;

    @Column(nullable = false)
    @NotNull(message = "accMsgBal cannot be null")
    private BigDecimal accMsgBal;
    private String accUsername;
    private String accPhysicalAddress;
    @Column(nullable = false)
    private BigDecimal accSmsPrice;
    private String accDeliveryUrl;
    private String accActivateNonSaf;
    private String accUseAlternativeSender;

    //    @Enumerated(EnumType.STRING)
    private AcStatus accStatus;
    @Column(nullable = false)
    @NotNull(message = "accAdminMobile cannot be null")
    private String accAdminMobile;
    @Column(nullable = false)
    @NotNull(message = "accAdminEmail cannot be null")
    private String accAdminEmail;

    private Long accReorderLevel;
    private String accKraFileName;
    private String accIncorporationCertFileName;
    private String accAuthorizationFileName;
    private Date accToDisableOnDate;
    private Integer accToDiableTimer;


    @OneToMany
    @JoinColumn(name = "sh_acc_id", referencedColumnName = "acc_id", insertable = false, updatable = false)
    private List<MsgShortcodeSetup> senderId;
}


//SMART_GATE_RESELLER_ID