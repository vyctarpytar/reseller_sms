package com.spa.smart_gate_springboot.dashboad;


import lombok.*;

@Data
@AllArgsConstructor
@Builder
public class  MsgStatusStat {
    private String msgStatus;
    private int msgCount;
    private String path ;
    private int msgPerCent ;
}
