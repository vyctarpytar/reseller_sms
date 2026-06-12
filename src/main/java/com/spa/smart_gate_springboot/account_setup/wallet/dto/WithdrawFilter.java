package com.spa.smart_gate_springboot.account_setup.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Payout-history filter. Field names mirror the legacy NdovuPay withdrawal filter the React
 * PayoutHistory page posts.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WithdrawFilter {
    private String withDrawStatus;
    private String withDrawSubmobileNo;
    private Date withDrawDateFrom;
    private Date withDrawDateTo;
    private int start;
    private int limit;
}
