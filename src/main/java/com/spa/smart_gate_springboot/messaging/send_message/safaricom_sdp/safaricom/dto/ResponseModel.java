    package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseModel {
    private String status;
    private String message;
    private Object data;
    @Builder.Default
    private boolean success = false;
    private int batchSize;
}
