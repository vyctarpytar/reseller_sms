package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CallBackResp {
//    {
//        "response-code": 200,
//            "message-id": 192325341,
//            "response-description": "Success",
//            "delivery-status": 32,
//            "delivery-description": "DeliveredToTerminal",
//            "delivery-tat": "0.47 sec",
//            "delivery-networkid": 2,
//            "delivery-time": "2025-11-24 23:30:28"
//    }

    @JsonProperty("response-code")
    public int responseCode;
    
    @JsonProperty("message-id")
    public String messageId;
    
    @JsonProperty("response-description")
    public String responseDescription;
    
    @JsonProperty("delivery-status")
    public int deliveryStatus;
    
    @JsonProperty("delivery-description")
    public String deliveryDescription;
    
    @JsonProperty("delivery-tat")
    public String deliveryTat;
    
    @JsonProperty("delivery-networkid")
    public int deliveryNetworkId;
    
    @JsonProperty("delivery-time")
    public String deliveryTime;
}
