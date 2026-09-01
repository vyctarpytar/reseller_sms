package com.spa.smart_gate_springboot.account_setup.senderId;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * One sender ID as offered in a "select sender name" list. Carries the network because the same
 * name is registered once per network it lives on — distinct on the name alone would collapse those
 * back into one entry and hide that a name covers e.g. both Safaricom and Airtel.
 */
@Data
@AllArgsConstructor
public class SenderNameDto {
    private String shCode;
    private String shMsnProvider;
}
