package com.spa.smart_gate_springboot.messaging.send_message;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(schema = "msg")
@Entity(name = "message_queue_arc")
public class MsgMessageQueueArc {
    @Id
    @GeneratedValue
    private UUID msgId;
    private String msgExternalId;
    @Column(nullable = false)
    @NotNull(message = "field cannot be null")
    private UUID msgAccId;
    private String msgCode;
    private BigDecimal msgUsrId;
    private Date msgCreatedDate;
    private String msgStatus;
    private Date msgDeliveredDate;
    private String msgSubMobileNo;
    private BigDecimal msgSubCatId;
    @Column(nullable = false)
    @NotNull(message = "msgSubMobileNo cannot be null")
    private String msgMessage;
    private BigDecimal msgCostId;
    private Long msgCampId;
    private Long msgThreadId;
    private int msgRetryCount;
    @Column(nullable = false)
    @NotNull(message = "msgClientDeliveryStatus cannot be null")
    private String msgClientDeliveryStatus;
    private Date msgThreadTime;
    private String msgSenderLevel;
    private String msgErrorCode;
    @Column(length = 10000)
    private String msgErrorDesc;
    private Date msgCreatedTime;
    private String msgWhyResent;
    private Long msgPriorityId;
    private UUID msgCreatedBy;
    private String msgCreatedByEmail;
    private UUID msgGroupId;
    private Boolean msgSentRetried = Boolean.FALSE;

    private String msgAccName;
    private String msgSenderIdName;
    private UUID msgResellerId;
    private String msgResellerName;
    private Integer msgPage;
    private String msgSourceIpAddress;
    private String msgRequestId;

}
