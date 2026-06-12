package com.spa.smart_gate_springboot.account_setup.wallet.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Reseller request to buy SMS units from TOP using cash-wallet balance (no STK).
 * Exactly one of {units, amount} is required; the other is derived from the reseller's unit price.
 */
@Data
public class BuyUnitsRequest {
    /** Number of units to buy. If null, derived from {@code amount} / reseller unit price. */
    private BigDecimal units;
    /** KSh to spend. If null, derived from {@code units} * reseller unit price. */
    private BigDecimal amount;
    /** Optional client idempotency key to make a double-submit a no-op. */
    private String idempotencyKey;
}
