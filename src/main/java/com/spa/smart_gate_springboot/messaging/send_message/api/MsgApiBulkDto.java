package com.spa.smart_gate_springboot.messaging.send_message.api;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MsgApiBulkDto {
    private String msgExternalId;
    @NotNull(message = "msgSubMobileNo cannot be null")
    private String[] msgMobileNos;
    @NotNull(message = "msg cannot be null")
    private String msgMessage;
    @NotNull(message = "msg cannot be null")
    private String msgSenderId;
}
