package com.spa.smart_gate_springboot.ndovuPay.withdraw;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class WithDrawNdovuPayLoad {
    private int apwOrgCode;
    private BigDecimal apwAmount;
    private String apwWalCode;
    private String apwNumber;
    private String apwDesc;
    private String apwReceiverName;
    private String apwType; //MPESA
}
