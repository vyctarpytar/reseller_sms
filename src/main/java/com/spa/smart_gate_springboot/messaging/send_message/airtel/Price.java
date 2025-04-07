package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Price{
    private BigDecimal pricePerLookup;
    private BigDecimal pricePerMessage;
    private String currency;
}
