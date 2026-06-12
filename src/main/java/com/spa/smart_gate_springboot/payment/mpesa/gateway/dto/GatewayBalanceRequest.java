package com.spa.smart_gate_springboot.payment.mpesa.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for the Waretech gateway account-balance inquiry ({@code POST mpesa/v2/balance}).
 * Only the paybill/shortcode is supplied; the gateway holds the initiator credentials internally.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayBalanceRequest {
    private String paybill;
}
