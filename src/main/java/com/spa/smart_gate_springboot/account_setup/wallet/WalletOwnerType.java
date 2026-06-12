package com.spa.smart_gate_springboot.account_setup.wallet;

/**
 * Owner tier of a cash wallet. TOP is the platform owner (singleton wallet),
 * RESELLER is a reseller (one wallet per reseller).
 */
public enum WalletOwnerType {
    RESELLER,
    TOP
}
