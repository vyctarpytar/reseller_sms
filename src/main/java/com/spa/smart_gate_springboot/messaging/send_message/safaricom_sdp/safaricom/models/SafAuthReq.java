package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.models;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class SafAuthReq {
    private String username;
    private String password;
}
