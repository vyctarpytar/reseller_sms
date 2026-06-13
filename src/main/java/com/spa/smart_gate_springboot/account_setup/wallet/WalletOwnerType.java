package com.spa.smart_gate_springboot.account_setup.wallet;

/**
 * Owner tier of a ledger line. TOP is the platform owner (singleton cash wallet), RESELLER is a
 * reseller (one cash wallet each). ACCOUNT has NO cash wallet — it appears only as the owner of
 * UNIT statement legs (units credited to an account on purchase).
 *
 * <p>Persisted as ORDINAL on {@code Wallet} (see its class comment), so new values MUST be appended
 * at the end to keep existing ordinals stable. On {@code WalletTransaction} it is persisted as STRING.
 */
public enum WalletOwnerType {
    RESELLER,
    TOP,
    ACCOUNT
}
