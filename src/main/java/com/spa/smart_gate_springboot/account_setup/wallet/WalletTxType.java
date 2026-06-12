package com.spa.smart_gate_springboot.account_setup.wallet;

/**
 * Ledger entry type for a cash wallet movement. Persisted as STRING for a readable ledger.
 * NOTE (ddl-auto:update + Postgres): adding a value here does NOT rebuild the
 * CHECK (tx_type IN (...)) constraint Hibernate created. Drop it on every DB:
 *   ALTER TABLE msg.cash_wallet_transaction DROP CONSTRAINT IF EXISTS cash_wallet_transaction_tx_type_check;
 */
public enum WalletTxType {
    /** Money in via M-Pesa STK collection. */
    DEPOSIT_MPESA,
    /** Cash credited to a wallet when units it sold are paid for (account buys units → reseller; reseller buys units → TOP). */
    UNIT_SALE_CREDIT,
    /** Cash debited from a reseller wallet when buying units from TOP using wallet balance. */
    UNIT_PURCHASE_DEBIT,
    /** Net payout leg of a withdrawal (gross − charge). */
    WITHDRAWAL,
    /** M-Pesa service charge leg of a withdrawal. */
    MPESA_CHARGE,
    /** Compensation credit when a B2C payout is rejected/failed. */
    WITHDRAWAL_REVERSAL,
    /** Compensation credit for the charge leg when a B2C payout is rejected/failed. */
    MPESA_CHARGE_REVERSAL,
    /** Manual admin correction. */
    ADJUSTMENT
}
