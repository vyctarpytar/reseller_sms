package com.spa.smart_gate_springboot.dashboad.annual;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SummaryDto {
    private String title;
    private String value;
    private String svg;
}
