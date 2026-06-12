package com.spa.smart_gate_springboot.payment.mpesa.b2c;

public enum B2cTransactionStatus {
    /** Accepted by the gateway, awaiting terminal result (polled). */
    PROCESSING,
    /** Gateway confirmed the payout reached the recipient. */
    SUCCESS,
    /** Gateway returned a non-zero result, or initiation was rejected. */
    FAILED
}
