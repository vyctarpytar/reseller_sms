package com.spa.smart_gate_springboot.payment;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "c2b_transaction", schema = "msg")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue
    private UUID id;
    private String transType;
    private String transId;
    private String transTime;
    private LocalDateTime transDateTime;
    @NotNull(message = "field cannot be null")
    private BigDecimal transAmount;
    private String businessShortCode;
    @NotNull(message = "field cannot be null")
    private String billRefNumber;
    private String invoiceNumber;
    private String msisdn;
    private String kycName;
    private String kycValue;
    private Integer resultCode;
    private String resultDesc;
    private String thirdPartyTransid;
    private String requestType;
    private String requestStatus;
    private String rcptId;
    private String c2bStatus;
    private String reversedBy;
    private LocalDateTime reversedDate;
    private LocalDateTime bankReversalDate;
    private LocalDateTime transactionDate;
    private String reversalReason;
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    private UUID transResellerId;
}

