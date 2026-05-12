package com.spa.smart_gate_springboot.messaging.send_message.safaricom_sdp.safaricom.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Datum{
    private String name;
    private String value;
}
