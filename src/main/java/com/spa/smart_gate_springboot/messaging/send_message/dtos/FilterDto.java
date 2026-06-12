package com.spa.smart_gate_springboot.messaging.send_message.dtos;

import lombok.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;


@Builder
@Data
@AllArgsConstructor
public class FilterDto {
    private String  msgStatus;
    // Bucket filter: when set, the SMS list/excel matches msg_status against any value in this list
    // (used by the summary-card quick filters, where one card maps to several raw statuses).
    private List<String> msgStatusList;
    private UUID msgCreatedBy;
    private UUID msgAccId;
    private String msgSenderId;
    private UUID msgGrpId;
    private Date msgCreatedDate;
    private Date msgCreatedFrom;
    private Date msgCreatedTo;
    private String msgSubmobileNo;
    private String msgMessage;
    private UUID msgSaleUserId;
    private UUID msgResellerId;
    private int start;
    private int limit;
    private String sortColumn;
}
