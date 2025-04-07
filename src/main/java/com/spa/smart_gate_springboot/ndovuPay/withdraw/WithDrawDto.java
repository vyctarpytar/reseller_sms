package com.spa.smart_gate_springboot.ndovuPay.withdraw;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@Data
@RequiredArgsConstructor
@Builder
public class WithDrawDto {
    private BigDecimal withDrawAmount;
    private String withDrawPhoneNumber;
    private String withDrawCode;
    private UUID withDrawLogId;
}


