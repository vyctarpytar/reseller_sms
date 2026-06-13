package com.spa.smart_gate_springboot.account_setup.credit;

// PENDING_APPROVAL removed with the manager-approval flow. Any legacy rows holding it MUST be
// remapped in the DB before this ships (see deploy notes), or @Enumerated(STRING) reads will fail.
public enum CrStatus{
    PROCESSED, REVERSED
}
