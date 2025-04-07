package com.spa.smart_gate_springboot.account_setup.request;

import com.spa.smart_gate_springboot.account_setup.reseller.Reseller;
import com.spa.smart_gate_springboot.account_setup.senderId.ShortCode;
import com.spa.smart_gate_springboot.account_setup.shortsetup.MsgShortcodeSetup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@Data
@RequiredArgsConstructor
@AllArgsConstructor
@Table(name = "service_request", schema = "js_core")
public class RequestEntity {

    @Id
    @GeneratedValue
    private UUID reId;
    private String reTelcos; // saf, airtel
    @Enumerated(EnumType.STRING)
    private SenderIdType reSenderIdType;
    private String reName;
    private String reDesc;
    private String reInstType;
    private String reSenderId;
    @NotNull(message = "reseller is null")
    private UUID reResellerId;
    @Enumerated(EnumType.STRING)
    private ServiceOwnership reServiceOwnership;
    @Enumerated(EnumType.STRING)
    private CostCover reCostCover;
    private String reKeyWord;
    private LocalDateTime reCreatedDate;
    private UUID reCreatedBy;
    @Enumerated(EnumType.STRING)
    private ReStatus reStatus;
    @Enumerated(EnumType.STRING)
    private ReServiceType reServiceType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reResellerId", referencedColumnName = "rsId", insertable = false, updatable = false)
    private Reseller reseller;


    private UUID reSetUpId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reSetUpId", referencedColumnName = "shId", insertable = false, updatable = false)
    private ShortCode resetup;

    private LocalDateTime reUpdatedDate;
    private UUID reUpdatedBy;


    private String reKraFileName;
    private String reIncorporationCertFileName;
    private String reAuthorizationFileName;
}


