package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.models.internal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class InternalTestSendSmsReq {
    @NotNull
    private String message;
    @NotNull
    private List<String> phoneNumbers;
    @NotNull
    private String senderId;

}


