package com.spa.smart_gate_springboot.messaging.send_message.dtos;

import lombok.*;

import java.util.Date;
import java.util.UUID;


@Builder
@Data
@AllArgsConstructor
public class FilterDto {
    private String  msgStatus;
    private UUID msgCreatedBy;
    private UUID msgAccId;
    private String msgSenderId;
    private UUID msgGrpId;
    private Date msgCreatedDate;
    private Date msgDateFrom;
    private Date msgDateTo;
    private String msgSubmobileNo;
    private String msgMessage;
    private UUID msgSaleUserId;
    private UUID msgResellerId;
    private int start;
    private int limit;
    private String sortColumn;
}
