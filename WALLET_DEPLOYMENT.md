# M-PESA Cash Wallet — Deployment & Test Runbook

This change adds a real M-PESA **cash wallet (KES)** for RESELLER and TOP, removes NdovuPay entirely,
and routes both STK collection and B2C payout through the **Waretech gateway**
(`https://c2b.waretechlimited.com`).

## 1. Prerequisites before first boot

- **JDK 17** to build (`JAVA_HOME=.../ms-17.0.18`). Default JDK 21 breaks Lombok.
- Confirm the Waretech gateway has shortcode **4037171** onboarded for **both** STK and B2C.
- Set `mpesa.gateway.b2cPaybill` (application.properties) to the correct B2C paybill if it differs
  from the STK shortcode. Set `mpesa.gateway.authToken` only if the deployed gateway requires a bearer token.

New tables are created automatically by Hibernate (`ddl-auto:update`) in schema **`msg`**:
`cash_wallet`, `cash_wallet_transaction`, `mpesa_b2c_charge`, `b2c_transaction` (the latter includes a
`reversed` boolean guarding against double-refunds). On startup, seeders populate the charge bands and
bootstrap a `TOP_PLATFORM` wallet + a wallet per reseller.

## 2. Run AFTER the first boot — DROP enum CHECK constraints (dev AND prod)

`ddl-auto:update` creates `CHECK (col IN (...))` constraints for STRING enums but never rebuilds them
when enum values are added later → future inserts of new enum values fail. Drop them once
(verify exact names first with `\d+ msg.<table>`):

```sql
ALTER TABLE msg.cash_wallet_transaction DROP CONSTRAINT IF EXISTS cash_wallet_transaction_tx_type_check;
ALTER TABLE msg.b2c_transaction         DROP CONSTRAINT IF EXISTS b2c_transaction_status_check;
ALTER TABLE msg.b2c_transaction         DROP CONSTRAINT IF EXISTS b2c_transaction_purpose_check;
```

> **PROD:** the CI/CD pipeline has no DB-migration step. These DROPs must be run **manually** on prod,
> or adding a new `WalletTxType` / B2C status / purpose later will silently break those inserts.

(`cash_wallet.owner_type` and `cash_wallet.status` are persisted as **ordinal** — no CHECK constraint.)

## 3. Optional legacy cleanup (after verifying the new flow)

`ddl-auto:update` does NOT drop removed columns/tables. Once happy:

```sql
-- migrate old payout history into cash_wallet_transaction first if you want to keep it, then:
ALTER TABLE js_core.reseller DROP COLUMN IF EXISTS rs_has_ndovu_pay_account;
DROP TABLE IF EXISTS msg.ndovu_pay_withdraw;
```

## 4. Money flows (what to expect)

| Event | Units | Cash wallet |
|---|---|---|
| Account buys units (STK→C2B) | account units ↑, reseller allocatable units ↓ | **reseller wallet credited** full KSh paid |
| Reseller self top-up (STK→C2B) | reseller allocatable units ↑ | **TOP_PLATFORM credited** KSh paid |
| Reseller buys units from wallet (`POST /api/v2/wallet/buy-units`) | reseller units ↑ | reseller wallet ↓, **TOP_PLATFORM ↑** (one tx) |
| Withdraw (reseller/TOP) | — | wallet ↓ gross; recipient gets gross − charge via B2C |

Only **cash** credits hit the wallet (guarded by `smsPaymentId != null`); manual unit grants do not.

## 5. Manual sandbox test plan

1. **Reseller STK top-up** → C2B callback → `GET /api/v2/wallet/balance` shows the deposit.
   Re-send the same C2B callback (same `transId`) → balance does **not** change (idempotency fix).
2. **Account buys units** (STK) → account `accMsgBal` ↑, reseller `rsAllocatableUnit` ↓,
   reseller `GET /api/v2/wallet/balance` ↑ by the full KSh.
3. **Reseller buys units from wallet** (`POST /api/v2/wallet/buy-units {units}`) → reseller wallet ↓,
   `TOP_PLATFORM` ↑, reseller allocatable units ↑. Double-submit with same units → second is a no-op
   (idempotent) — confirm only one debit.
4. **Withdraw** (`POST /api/v2/wallet/initiate-withdraw` → SMS OTP → `POST /api/v2/wallet/finalize-withdraw`):
   wallet ↓ gross; B2C accepted (ConversationID); poller flips to SUCCESS with receipt within ~10s.
5. **Withdraw failure** (point gateway at a failing endpoint) → both ledger lines reversed, balance restored.
6. **TOP withdrawal** from `TOP_PLATFORM`.

## 6. Frontend

The React app (`reseller_sms_react_js`) withdrawal page + `billing` slice were repointed from
`/api/v2/ndovupay/*` to `/api/v2/wallet/*` (response shapes preserved). A new **Buy Units** button
(WithdrawalHeader → BuyUnitsModal) posts to `/api/v2/wallet/buy-units`. The withdrawal logo is now M-PESA.
Run `CI=false npm run build` to produce a deployable build.
