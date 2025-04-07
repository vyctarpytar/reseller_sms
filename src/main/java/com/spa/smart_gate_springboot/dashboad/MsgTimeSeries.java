package com.spa.smart_gate_springboot.dashboad;

import lombok.*;

@Getter
@Setter
@Data
@Builder
public class MsgTimeSeries{
    private String msgCreatedDate;
    private String msgStatus;
    private int msgCount;
}
