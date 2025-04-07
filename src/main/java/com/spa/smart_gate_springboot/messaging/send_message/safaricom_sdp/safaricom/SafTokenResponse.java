package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom;

import lombok.Data;

@Data
public class SafTokenResponse {
    private String msg;
    private String token;
    private String refreshToken;
    private String message;
    private int errorCode;
    private int status;
    private String timestamp;
}
