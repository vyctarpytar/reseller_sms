package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.models;

import lombok.Data;

@Data
public  class SafBulkDataSet {
    private String userName;
    private String channel;
    private String packageId;
    private String oa;//SENDER ID
    private String msisdn;
    private String message;
    private String uniqueId;
    private String actionResponseURL;


}
