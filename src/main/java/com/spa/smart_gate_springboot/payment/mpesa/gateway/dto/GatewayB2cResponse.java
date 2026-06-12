package com.spa.smart_gate_springboot.payment.mpesa.gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayB2cResponse {

    @JsonProperty("ConversationID")
    private String conversationId;

    @JsonProperty("OriginatorConversationID")
    private String originatorConversationId;

    @JsonProperty("ResponseCode")
    private String responseCode;

    @JsonProperty("ResponseDescription")
    private String responseDescription;

    public boolean isAccepted() {
        return "0".equals(responseCode);
    }
}
