package com.spa.smart_gate_springboot.payment.mpesa.gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Account-balance inquiry response from the Waretech gateway. {@code workingBalance} is the
 * spendable B2C float; {@code utilityBalance} is the paybill collection account.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayBalanceResponse {

    @JsonProperty("shortcode")
    private String shortcode;

    @JsonProperty("utilityBalance")
    private BigDecimal utilityBalance;

    @JsonProperty("workingBalance")
    private BigDecimal workingBalance;
}
