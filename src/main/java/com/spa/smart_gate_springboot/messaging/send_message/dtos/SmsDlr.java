package com.spa.smart_gate_springboot.messaging.send_message.dtos;

import lombok.Data;

@Data
public class SmsDlr {
    private String requestId;
    private String requestTimeStamp;
    private String channel;
    private String operation;
    private String traceID;
    private RequestParam requestParam;
}