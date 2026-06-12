package com.spa.smart_gate_springboot.payment.mpesa.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * B2C payout request body for the Waretech gateway {@code POST /mpesa/v2/b2c}.
 * {@code commandId} is typically "BusinessPayment".
 */
@Data
@Builder
public class GatewayB2cRequest {
    private String paybill;
    private String phoneNumber;
    private BigDecimal amount;
    private String commandId;
    private String remarks;
    private String occasion;
}
