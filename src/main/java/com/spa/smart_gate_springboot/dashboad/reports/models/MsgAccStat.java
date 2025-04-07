package com.spa.smart_gate_springboot.dashboad.reports.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class  MsgAccStat {
    private String msgAccName;
    private int msgCount;
    private String path ;
    private int msgPerCent ;
}
