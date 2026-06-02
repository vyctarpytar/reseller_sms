package com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SafaricomRestSendRequest {
    private Long timeStamp;
    private List<DsdpBulkDataSet> dataSet;
}
