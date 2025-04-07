package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.dto;

import lombok.Data;

@Data
public class SmsResponse{
    private String requestId;
    private String requestTimeStamp;
    private String channel;
    private String operation;
    private String traceID;
    private RequestParam requestParam;
}