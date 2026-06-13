# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Multi-tenant **reseller SMS platform** (brand: Synq Africa). A Spring Boot backend (this repo root,
Maven, `com.spa.smart_gate_springboot`) plus a React + Vite frontend in [frontend/](frontend/) which
has **its own [frontend/CLAUDE.md](frontend/CLAUDE.md)** — read that for anything UI-side.

The platform sells SMS through a three-tier hierarchy: **TOP** (the platform operator) → **RESELLER**
→ **ACCOUNT** (end customer). Resellers buy SMS units (and cash) from TOP; accounts buy units from
their reseller; everyone sends SMS that gets dispatched through mobile-network operators (Safaricom,
Airtel) and billed against unit balances. Money moves via M-PESA (STK collection in, B2C payout out).

## Commands

This is a Maven project (`./mvnw` wrapper). **Build with JDK 17** — the default JDK 21 on this machine
breaks Lombok annotation processing. Set `JAVA_HOME` to a 17 JDK first.

```bash
# build (CI uses -DskipTests)
JAVA_HOME=<jdk-17> ./mvnw clean package -DskipTests

# run locally (needs local Postgres `synq_africa_rds` + RabbitMQ, see application.properties)
JAVA_HOME=<jdk-17> ./mvnw spring-boot:run

# tests (spring-boot-starter-test is present; CI skips them)
JAVA_HOME=<jdk-17> ./mvnw test
JAVA_HOME=<jdk-17> ./mvnw test -Dtest=SomeClass#someMethod   # single test
```

Frontend commands live in [frontend/CLAUDE.md](frontend/CLAUDE.md) (`npm run dev` / `build`, **Vite 8**).

App listens on `server.port=8443`. Swagger UI is served (springdoc) and is in the security white-list.

## Architecture

Entry point: [Smart_gate_spring_boot.java](src/main/java/com/spa/smart_gate_springboot/Smart_gate_spring_boot.java)
(`@SpringBootApplication`, `@EnableScheduling`, forces TZ Africa/Nairobi). Source is organized by
**domain package**, not by layer: `auth`, `user`, `account_setup/*` (reseller, account, wallet, credit,
invoice, senderId, group, member, blacklist, request), `messaging/*` (send_message, sender, shedules,
templates, delivery, operatorPrefix), `payment/*`, `pushSDK/daraja`, `dashboad`, `report`, `menu`,
`crons`, `MQRes`, `config`.

### Auth & multi-tenancy
- **Stateless JWT.** Custom login issues a JWT (jjwt) carrying `layer` (TOP/RESELLER/ACCOUNT), `role`,
  `authorities`, `changePassword`, and a `redirectUrl`. Config in `config/SecurityConfiguration.java`,
  `config/JwtAuthenticationFilter.java`, `config/JwtService.java`. Issued tokens are also persisted
  (`user/token`) with `expired`/`revoked` flags so logout can revoke them.
- **Public (no-auth) URL prefixes**: `/auth/**`, `/api/v2/public/**` (DLR & inbound callbacks),
  `/api/v2/payment/**` (M-PESA webhooks), `/api/v2/sandbox/**` (API-key sandbox), Swagger,
  `/actuator/prometheus`. Everything else requires a bearer token.
- **Tenant scoping is enforced per-controller, not by a global filter.** The frontend sends
  `reseller_id` and `account_id` as request params on every call; controllers read the current user
  (`UserService.getCurrentUser`) and validate drill-down against the user's layer via
  `AccountService.resolveAccountScope(user, accountId)`: ACCOUNT users are pinned to their own account
  (params ignored), RESELLER users may drill into accounts that belong to their reseller (else
  `AccessDeniedException`), TOP users may access anything. When adding a query that returns
  tenant-owned data, scope it the same way — do not trust the raw param.

### API versioning
Controllers hardcode their version in `@RequestMapping`. **`/api/v2/**` is the live surface** (~22
controllers: `sms`, `wallet`, `credit`, `invoice`, `dash`, `rpt`, `users`, `schedule`, `groups`,
`shortcode`, `setup`, `req`, `payment`, `public`, `sandbox`, `api-key`, …). `/api/v1/**` is legacy/admin
only (`menu`, `blacklist`, `management/**` which is role-gated, `auth/logout`). New endpoints go on v2.

### SMS pipeline (async via RabbitMQ)
1. A send hits a controller in `messaging/send_message` (`MessageController` for the web UI:
   single/group/multi-group/CSV; `IncomingController` for `/api/v2/public/**`; `ApiController` for the
   API-key sandbox). `QueueMsgService` validates balance and **publishes to RabbitMQ** (`MQRes/RMQPublisher`).
2. A `@RabbitListener` consumer (`messaging/send_message/MQReceiverSynq`) deserializes the message,
   recomputes cost, checks/debits credit, and **persists each SMS** to `msg.message_queue_arc`
   (entity `MsgMessageQueueArc`, status starts `PENDING_PROCESSING`).
3. The **provider router** (`safaricom_sdp/SafBulkService`) picks the carrier: property
   `safaricom.api.version` selects Safaricom **SDP v1** vs **Daraja REST v2**
   (`safaricom_rest/SafaricomRestBulkService`); `sms.airtel.allowForAll` / per-number MNO lookup in
   `messaging/operatorPrefix` (`operator_prefix` table) routes to **Airtel** (`airtel/AiretelService`).
   Infobip is also a wired provider. Sender-ID / package (TRANSACTIONAL vs PROMOTIONAL) comes from the
   account's shortcode setup. (An Infobip Java client is also a dependency under `messaging`.)
4. **Delivery reports**: carriers POST to `/api/v2/public/dlr` (`IncomingController`), which republishes
   to a DLR queue consumed by `SafDlrService`, which updates `message_queue_arc` by `msg_code`+msisdn.
5. **Client callbacks**: for API-originated sends with a `msgCallbackUrl`, the `crons` package
   (`ClientDeliveryResponses`) forwards delivery status to the client (retry/backoff, stuck-message
   handling).

Scheduled/bulk: `messaging/shedules/ScheduleService` dispatches `msg_schedule` rows when their release
time matches; `messaging/send_message/SchedulingConfig` retries failed sends (with Airtel fallback).

### Wallet / billing (double-entry ledger) — see [WALLET_DEPLOYMENT.md](WALLET_DEPLOYMENT.md)
A real **M-PESA cash wallet (KES)** for RESELLER and TOP, plus SMS-unit inventory, recorded in one
append-only ledger. Core in `account_setup/wallet`:
- `cash_wallet` (`Wallet`) — one per reseller (`walletCode = RS_<rsId>`) + a `TOP_PLATFORM` singleton;
  cash `balance`/`lockedBalance`, optimistic `@Version`.
- `cash_wallet_transaction` (`WalletTransaction`) — signed legs with `valueType` **KSH or UNIT**,
  `txType` (`WalletTxType`), `balanceAfter` (running balance), and a **unique `externalRef`** used as an
  **idempotency key** (M-PESA transId, withdrawal ref, unit-purchase UUID). `WalletService.credit/debit`
  take a row lock and are idempotent on `externalRef`.
- **Money flows** (the table in WALLET_DEPLOYMENT.md is authoritative): account buys units (STK→C2B) →
  reseller cash wallet credited, reseller allocatable units ↓, account units ↑; reseller self top-up →
  TOP wallet credited; reseller buys units from wallet → reseller wallet ↓ + TOP ↑; withdrawal → wallet
  ↓ gross, recipient gets gross − M-PESA charge via B2C. **Only cash credits hit the wallet (guarded by
  `smsPaymentId != null`)** — manual unit grants do not.
- **M-PESA** goes through the **Waretech gateway** (`payment/mpesa/gateway/WaretechMpesaService`,
  `mpesa.gateway.*`) for STK collection and B2C payout; `pushSDK/daraja/DarajaService` is the older
  direct-Daraja path (deprecated). Withdrawals are OTP-gated, debit payout+charge atomically, and a cron
  (`crons/MpesaB2cPayoutsCron`, every 10s) polls B2C status; failed B2C is reversed atomically (guarded
  by `B2cTransaction.reversed`). Settlement/invoicing in `account_setup/invoice` + `account_setup/credit`.

## Database & persistence conventions

- **Postgres** `synq_africa_rds`, two schemas: **`msg`** (messaging, wallet, b2c, invoices) and
  **`js_core`** (reseller, accounts, credit, operator prefixes). `ddl-auto: update`. KES currency,
  Africa/Nairobi timezone enforced at the JVM, Hikari, and session level.
- **Entities override the JPA name to a snake_case table**: `@Entity(name = "message_queue_arc")`,
  `@Entity(name = "jsc_accounts")`, etc. **JPQL `@Query` must use that entity name, not the Java class
  name** — otherwise the app aborts on boot with `UnknownEntityException`.
- **`ddl-auto: update` + enum columns is a recurring trap** (this is the project's #1 deploy gotcha):
  - Adding/removing a value of a `@Enumerated(STRING)` enum does **not** rebuild the Postgres
    `CHECK (col IN (...))` constraint, so inserts of the new value fail (`violates check constraint`).
    The fix is to **DROP the check constraint** on every environment (dev **and** prod). CI has **no DB
    migration step**, so prod DROPs are manual — see the exact statements in
    [WALLET_DEPLOYMENT.md](WALLET_DEPLOYMENT.md) §2.
  - Removing an enum constant also orphans existing rows (reads of old values crash) — remap them in SQL.
  - `update` never drops removed columns/tables — clean those up manually (WALLET_DEPLOYMENT.md §3).
- **Optional/dynamic filters use JPA `Specification` + `findAll(spec, …)`**, not
  `(:param IS NULL OR col = :param)` JPQL — the latter breaks on Postgres with type-inference error 42P18.

## Deploy & CI (push to `main` deploys)

A push to `main` triggers CI, **split by path** so frontend and backend deploy independently:
- [.github/workflows/deploy.yml](.github/workflows/deploy.yml) — **backend**. Ignores `frontend/**` and
  the workflow files. Builds `mvn clean package -DskipTests` (JDK 17), SCPs the jar to **`/opt/apps/`**
  on the VM, and `systemctl restart sms-app`. ⚠️ The deploy path must match the systemd unit's
  `ExecStart` (`systemctl cat sms-app`) — a `/opt/apps` vs `/opt/app` mismatch silently runs a stale jar.
- [.github/workflows/react.yml](.github/workflows/react.yml) — **frontend**. Runs only on `frontend/**`.
  `npm ci` + `npm run build` (Node 22, `CI=false`) → copies `frontend/build/*` to `/var/www/html/sms`
  and reloads nginx.

`application.properties` currently holds **live secrets in plaintext** (DB, JWT, RabbitMQ, Safaricom,
M-PESA gateway tokens) — do not echo them into logs/commits, and prefer redaction when reading the file.

## See also

- [frontend/CLAUDE.md](frontend/CLAUDE.md) — the React/Vite app (routing, Redux, axios tenant params, Vite 8).
- [WALLET_DEPLOYMENT.md](WALLET_DEPLOYMENT.md) — wallet/M-PESA runbook + the manual DB steps prod needs.
