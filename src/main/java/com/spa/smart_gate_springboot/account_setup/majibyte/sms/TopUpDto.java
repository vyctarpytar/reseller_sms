package com.spa.smart_gate_springboot.account_setup.majibyte.sms;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@RequiredArgsConstructor
public class TopUpDto{
    @NotNull(message = "Field Cannot be null or less than 100")
    @Min(100)
    private BigDecimal smsPayAmount;
    @NotNull(message = "Field Cannot be null")
    private String smsPayerMobileNumber;

    @NotNull(message = "Field Cannot be null")
    private String smsAccId; //UUID
}
