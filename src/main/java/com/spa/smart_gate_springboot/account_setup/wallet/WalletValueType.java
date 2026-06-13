package com.spa.smart_gate_springboot.account_setup.wallet;

/**
 * What a statement ledger row measures: real cash (KSH) or SMS units (UNIT). Drives the value-type
 * filter on the wallet statement. Stored as STRING on a NEW column, so its Hibernate-generated CHECK
 * constraint is created fresh with both values — no manual DROP needed (unlike {@link WalletTxType}).
 */
public enum WalletValueType {
    /** A cash movement (Kenyan shillings). */
    KSH,
    /** An SMS-unit movement (inventory). */
    UNIT
}
