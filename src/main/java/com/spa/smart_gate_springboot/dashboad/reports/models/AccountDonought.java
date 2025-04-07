package com.spa.smart_gate_springboot.dashboad.reports.models;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Data
@RequiredArgsConstructor
public class AccountDonought {
    List<MsgAccStat> accStatList;
}


