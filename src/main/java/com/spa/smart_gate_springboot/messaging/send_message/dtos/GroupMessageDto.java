package com.spa.smart_gate_springboot.messaging.send_message.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class GroupMessageDto{
    @NotNull(message = "grpMessage cannot be null")
    private String grpMessage;
    private String grpSendAt;
    private boolean grpAddMsgToTemplate;
    @NotNull(message = "field cannot be null")
    private String senderId;
    private String grpIds;
    private String sourceIpAdd;

}
