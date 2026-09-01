package com.spa.smart_gate_springboot.account_setup.senderId;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Re-points an already-registered sender ID at a different network. {@code register} refuses a
 * sender ID that already exists, so this is the only way to correct one created before
 * {@code sh_msn_provider} existed (those were all stamped SAFARICOM by the backfill).
 */
@Data
@RequiredArgsConstructor
public class ShortCodeProviderDto {
    @NotNull(message = "shCode Cannot Be Null")
    private String shCode;

    @NotNull(message = "shMsnProvider Cannot Be Null")
    private MsnProvider shMsnProvider;
}
