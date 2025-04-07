package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.models.internal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SingleSms {

    @NotNull
    private String message;

    @NotNull
    private String phoneNumbers;

    private String senderId;

    private String profileSid;


}


