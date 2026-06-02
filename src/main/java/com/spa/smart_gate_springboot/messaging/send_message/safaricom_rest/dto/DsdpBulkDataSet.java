package com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DsdpBulkDataSet {
    private String userName;
    private String channel;
    private String packageId;
    private String oa;
    private String msisdn;
    private String message;
    private String uniqueId;
    private String actionResponseURL;
    private String hashed;
}
