package com.spa.smart_gate_springboot.messaging.send_message.safaricom_rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SafaricomRestLoginRequest {
    private String username;
    private String password;
}
