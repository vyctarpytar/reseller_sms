package com.spa.smart_gate_springboot.messaging.send_message.airtel;

import lombok.Data;

@Data
public class Status{
    private String groupName;
    private Integer groupId;
    private String name;
    private String description;
    private String action;
    private Integer id;
}
