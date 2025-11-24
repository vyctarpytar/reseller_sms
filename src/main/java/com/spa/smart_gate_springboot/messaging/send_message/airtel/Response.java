package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data

public class Response{
    @JsonProperty("response-code")
    public int responseCode;

    @JsonProperty("response-description")
    public String responseDescription;
    public long mobile;
    public String messageid;
    public int networkid;
}
