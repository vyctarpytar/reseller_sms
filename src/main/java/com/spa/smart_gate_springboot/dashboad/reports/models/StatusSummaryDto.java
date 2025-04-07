package com.spa.smart_gate_springboot.dashboad.reports.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class StatusSummaryDto {
    private String msgStatus;
    private int noOfMessages;
    private BigDecimal credit;
}
