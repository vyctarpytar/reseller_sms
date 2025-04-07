package com.spa.smart_gate_springboot.dashboad.reports.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class DailyMessageSumDto {
    private Date createdDate;
    private int noOfMessages;
    private BigDecimal credit;
}


