package com.spa.smart_gate_springboot.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentDto {

    @JsonProperty("TransactionType")
    private String transType;
    
    @JsonProperty("TransID")
    private String transId;
    
    @JsonProperty("TransTime")
    private String transTime;
    
    @JsonProperty("TransAmount")
    @NotNull(message = "Transaction amount cannot be null")
    private BigDecimal transAmount;
    
    @JsonProperty("BusinessShortCode")
    private String businessShortCode;
    
    @JsonProperty("BillRefNumber")
    @NotNull(message = "Bill reference number cannot be null")
    private String billRefNumber;

    
    @JsonProperty("OrgAccountBalance")
    private String orgAccountBalance;
    
    @JsonProperty("ThirdPartyTransID")
    private String thirdPartyTransId;
    
    @JsonProperty("MSISDN")
    private String msisdn;
    
    @JsonProperty("FirstName")
    private String firstName;
    
    @JsonProperty("MiddleName")
    private String middleName;
    
    @JsonProperty("LastName")
    private String lastName;

    // Additional fields that might be useful for internal processing
//    private LocalDateTime transDateTime;
//    private Integer resultCode;
//    private String resultDesc;
//    private String requestType;
//    private String requestStatus;
//    private String rcptId;
//    private String c2bStatus;
//    private String reversedBy;
//    private LocalDateTime reversedDate;
//    private LocalDateTime bankReversalDate;
//    private LocalDateTime transactionDate;

}

