package com.spa.smart_gate_springboot.account_setup.invoice;

public enum InvoStatus {
    PENDING_PAYMENT, PARTIALLY_PAID, PAID, FAILED_TO_POP_SDK,
    // STK push terminal states reported by the Safaricom stkCallback (ResultCode != 0)
    CANCELLED,   // user cancelled the SIM prompt (ResultCode 1032)
    FAILED,      // other STK failure (wrong PIN, insufficient funds, unreachable, etc.)
    EXPIRED      // no callback ever arrived; swept by the expiry cron after the due date
}
