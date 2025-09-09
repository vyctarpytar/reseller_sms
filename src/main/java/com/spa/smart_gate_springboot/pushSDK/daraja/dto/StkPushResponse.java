package com.spa.smart_gate_springboot.pushSDK.daraja.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StkPushResponse {
    
    @JsonProperty("MerchantRequestID")
    private String merchantRequestID;
    
    @JsonProperty("CheckoutRequestID")
    private String checkoutRequestID;
    
    @JsonProperty("ResponseCode")
    private String responseCode;
    
    @JsonProperty("ResponseDescription")
    private String responseDescription;
    
    @JsonProperty("CustomerMessage")
    private String customerMessage;
}
