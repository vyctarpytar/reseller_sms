package com.spa.smart_gate_springboot.messaging.send_message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MsgQueue {
    @Id
    @GeneratedValue
    private UUID msgId;

    @NotNull(message = "msgAccId cannot be null")
    private UUID msgAccId;
    private String msgCode;
    private String msgExternalId;
    private BigDecimal msgUsrId;
    private Date msgCreatedDate;
    private String msgStatus;
    private Date msgDeliveredDate;
    @NotNull(message = "msgSubMobileNo cannot be null")
    private String msgSubMobileNo;
    private BigDecimal msgSubCatId;

    @NotNull(message = "msg cannot be null")
    private String msgMessage;
    private BigDecimal msgCostId;
    private Long msgCampId;
    private Long msgThreadId;
    private Long msgRetryCount;
    private Date msgThreadTime;
    private String msgClientDeliveryStatus;
    private String msgSenderLevel;
    private String msgErrorCode;
    private String msgErrorDesc;
    private String msgCreatedTime;
    private String msgWhyResent;
    private Long msgPriorityId;
    private UUID msgCreatedBy;
    private UUID msgGroupId;
    private Boolean msgSentRetried;
    private String msgSenderId;
    private String msgCreatedByEmail;
    private Integer msgPage;
    private String msgAccName;
    private String msgSenderIdName;
    private UUID msgResellerId;
    private String msgResellerName;
    private String msgSourceIpAddress;
}

