package com.spa.smart_gate_springboot.account_setup.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Withdrawal request/echo payload. Field names mirror the legacy NdovuPay contract so the existing
 * React withdrawal page + OTP modal work unchanged.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WithdrawDto {
    private BigDecimal withDrawAmount;
    private String withDrawPhoneNumber;
    private String withDrawCode;     // OTP entered on finalize
    private UUID withDrawLogId;      // correlates initiate → finalize
}
