package com.spa.smart_gate_springboot.account_setup.account.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class AccBalanceUpdate{
    private UUID accId;
    private BigDecimal msgCost;
    private UUID accResellerId;
}
