package com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SafaricomRestSendResponse {
    private String keyword;
    private String status;
    private String statusCode;
    private String message;
}
