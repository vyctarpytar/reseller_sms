package com.spa.smart_gate_springboot.account_setup.invoice;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@RequiredArgsConstructor
public class CreditInvoDto{
    @NotNull(message = "Field Cannot be null")
    @Min(0)
    private BigDecimal smsPayAmount;
    @NotNull(message = "Field Cannot be null")
    private String smsPayerMobileNumber;

    private String smsLoadingMethod;
}


