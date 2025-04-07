package com.spa.smart_gate_springboot.messaging.send_message.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.*;


@Getter
@Setter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class SingleMessageDto{
    @NotNull(message = "field cannot be null")
    private String message;
    @NotNull(message = "field cannot be null")
    private String mobile;
    @NotNull(message = "field cannot be null")
    private String senderId;
    private String sendAt;
    private String sourceIpAdd;
}