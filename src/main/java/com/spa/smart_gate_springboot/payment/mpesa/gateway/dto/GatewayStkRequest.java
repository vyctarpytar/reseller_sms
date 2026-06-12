package com.spa.smart_gate_springboot.payment.mpesa.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * STK push request body for the Waretech gateway {@code POST /mpesa/v2/stkpush}.
 * The gateway holds Safaricom credentials internally; we send only these fields.
 */
@Data
@Builder
public class GatewayStkRequest {
    private String shortcode;
    private String phoneNumber;
    private BigDecimal amount;
    private String accountReference;
    private String description;
}
