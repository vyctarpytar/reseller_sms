package com.spa.smart_gate_springboot.account_setup.invoice;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class InvoiceResellerSummary {
    private BigDecimal amount;
    private String monthName;
    private int monthId;
}
