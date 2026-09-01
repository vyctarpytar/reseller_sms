package com.spa.smart_gate_springboot.account_setup.senderId;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ShortCodeDto{
    @NotNull(message =  "shCode Cannot Be Null")
    private String shCode;

    @NotNull(message =  "shSenderType Cannot Be Null")
    private String shSenderType;

    /** Mobile service network to register this sender ID on. Optional — defaults to SAFARICOM. */
    private MsnProvider shMsnProvider;
}
