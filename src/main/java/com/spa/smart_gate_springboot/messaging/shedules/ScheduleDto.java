package com.spa.smart_gate_springboot.messaging.shedules;

import lombok.*;

import java.util.UUID;


@AllArgsConstructor
@Data
@Builder
public class ScheduleDto {
    private UUID schId;
    private String schMessage;
    private String schReleaseTime;


}

