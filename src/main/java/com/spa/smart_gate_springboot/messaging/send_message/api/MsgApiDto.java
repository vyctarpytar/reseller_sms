package com.spa.smart_gate_springboot.messaging.send_message.api;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MsgApiDto {
    private String msgExternalId;
    @NotNull(message = "msgSubMobileNo cannot be null")
    private String msgMobileNo;
    @NotNull(message = "msg cannot be null")
    private String msgMessage;
    @NotNull(message = "msg cannot be null")
    private String msgSenderId;

    private String apiKey;
}



