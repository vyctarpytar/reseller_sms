package com.spa.smart_gate_springboot.messaging.shedules;

import lombok.Data;

import java.util.UUID;

@Data
public class ScheduleFilterDto{
    private UUID schUsrId;
    private UUID schAccId;
    private UUID schGrpId;
    private int start;
    private int limit;
}
