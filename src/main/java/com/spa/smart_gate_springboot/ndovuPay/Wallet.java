package com.spa.smart_gate_springboot.ndovuPay;

import lombok.*;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class Wallet {
    private String walAmount;
    private String walCurrency;
    private String walType;
    private String walCode;
}
