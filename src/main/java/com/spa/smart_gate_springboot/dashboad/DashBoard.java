package com.spa.smart_gate_springboot.dashboad;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Data
@RequiredArgsConstructor
public class DashBoard {
    List<MsgStatusStat> ststusSummary;
    List<MsgTimeSeries> msgTimeSeries;
}

