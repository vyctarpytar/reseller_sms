package com.spa.smart_gate_springboot.account_setup.account.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Date;

@Builder
@Getter
@Data
public class BalanceDto{
    private String accName;
    private String accResellerName;
    private String accStatus;
    private BigDecimal accBalance;
    private BigDecimal rsAllocatableMsgBal;
    private BigDecimal accRate;
    private long accUnits;
    private Date accToDisableOnDate;
    private int accToDiableTimer ;
}
