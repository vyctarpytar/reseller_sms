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
(`@SpringBootApplication`, `@EnableScheduling`; its first statement is `AppTime.install()`, which pins the
JVM to Africa/Nairobi — see **Time** below). Source is organized by
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
   account's shortcode setup. **Sender IDs are per-network**: `msg.shortcode`/`msg.shortcode_setup`
   carry `sh_msn_provider` (enum `MsnProvider`: SAFARICOM/AIRTEL/TELKOM), and `AiretelService` picks
   the account's — else the reseller's — sender ID registered on AIRTEL, falling back to
   `sms.airtel.defaultSenderId`. A Safaricom sender ID is not valid on Airtel, so never reuse the one
   that arrived on the message. **The network is part of a sender ID's identity**: the same name is
   registered once per network it lives on (MERIDIANBET on Safaricom and on Airtel are two rows), so
   uniqueness is `(sh_code, sh_reseller_id, sh_msn_provider)` on the registry and
   `(sh_code, sh_acc_id, sh_msn_provider)` on the mappings — never on `sh_code` alone, and assigning
   by name maps every network variant. Rows predating the column are stamped SAFARICOM, and the old
   pre-network UNIQUEs are swept, by an idempotent boot-time routine
   (`ShortCodeService.backfillMsnProvider` / `dropLegacySenderIdUniques`) — `ddl-auto: update` only
   ever ADDS constraints, so without that sweep the old ones survive and still reject the second
   network. No manual DB step in any environment. (An Infobip Java client is also a dependency under `messaging`.)
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

## Time

**Every timestamp comes from the JVM in `Africa/Nairobi` — never from the DB.** Use
[`AppTime.now()` / `AppTime.today()` / `AppTime.nowDate()`](src/main/java/com/spa/smart_gate_springboot/utils/AppTime.java),
not `LocalDateTime.now()` / `LocalDate.now()` / `new Date()`; the bare JDK calls read
`ZoneId.systemDefault()`, a process-global a container image or a stray `-Duser.timezone` can move.
`AppTime.install()` is the first statement of `main()` because things that read the JVM default can't be
parameterised: Hibernate's `@CreationTimestamp`/`@UpdateTimestamp` (VM clock, captured at bootstrap),
`SimpleDateFormat`, `Calendar`, pgjdbc's binding of `java.util.Date`, and log timestamps.

**No query may read the database clock.** `now()`, `current_timestamp` and `current_date` resolve
against the *Postgres session* zone, which is UTC on a stock RDS instance — that is exactly how a
created/delivered date lands three hours behind EAT. Every such call site is now a bound parameter fed
from `AppTime` (`updateDeliverNote`'s `:deliveredDate`; the `current_date - N` retry/callback windows in
`MsgMessageQueueArcRepository`; `AccountRepository.findAccountsForLowBalanceAlert`;
`InvoiceRepository.getResellerInvoicesPerYearSummary`; `ApiKeyRepository.existsValidApiKey`). Keep it
that way — and never give a column a `DEFAULT now()`.

All the Postgres columns involved are `timestamp without time zone` bound from `LocalDateTime` or
`java.util.Date`, so pgjdbc writes wall-clock verbatim and the server's own TZ never re-interprets it —
the stored values are EAT. `hibernate.jdbc.time_zone`, `spring.jackson.time-zone` and the Hikari
`connection-init-sql` are insurance for the day a column becomes `timestamptz`; they are not what makes
this correct. `AppTimeTest` locks the invariant in.

The SMPP gateway (`sms_smmp_gateway`) pins the same zone in its own `main()` and reads
`msg.message_queue_arc.msg_delivered_date` straight back out into the downstream `done date:` SMPP
receipt field — that contract breaks if this service ever changes zone.

## Database & persistence conventions

- **Postgres** `synq_africa_rds`, two schemas: **`msg`** (messaging, wallet, b2c, invoices) and
  **`js_core`** (reseller, accounts, credit, operator prefixes). `ddl-auto: update`. KES currency,
  Africa/Nairobi timezone enforced at the JVM, Hikari, and session level — see **Time** below.
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

### Credentials (none are in the repo)

`application.properties` carries `${DB_PASSWORD}`, `${RABBITMQ_PASSWORD}`, `${REDIS_PASSWORD}`,
`${JWT_SECRET_KEY}`, `${SAF_SDP_PASSWORD}` and `${SAF_REST_PASSWORD}` with **no defaults**, so an
unset one aborts startup instead of running half-configured against prod.

- **prod** — GitHub Actions repo secrets of the same names. `deploy.yml`'s "Push secrets to VM" step
  writes them over SSH to `/opt/apps/sms-app.env`, mode 0600, using **no sudo** (the "Prepare app dir"
  step already chowns `/opt/apps` to the deploy user; that VM grants NOPASSWD sudo for a handful of
  commands only, so `sudo install`/`tee` into `/etc` would hang on a password prompt). Values are
  written single-quoted because systemd's `EnvironmentFile` parser treats a bare `#` as a comment —
  `SAF_SDP_PASSWORD` contains one. The step then installs
  `/etc/systemd/system/sms-app.service.d/10-env.conf` with `EnvironmentFile=` via `sudo -n`
  (a drop-in, so the unit file is never rewritten by CI; re-applied every deploy so a rebuilt VM
  self-heals), and **aborts before the restart** if `systemctl show sms-app -p EnvironmentFiles`
  doesn't list it — otherwise the placeholders couldn't resolve and the app would die on boot.
- **local** — `application-dev.properties` (gitignored) supplies them; run with the `dev` profile.
  It overrides the datasource/RabbitMQ/Redis passwords with local ones and carries the JWT and
  Safaricom values verbatim.

Adding a secret: placeholder here, `printf` line in that step, `envs:`/`env:` entries, `gh secret set`,
and a line in `application-dev.properties`. **`deploy.yml` is in its own `paths-ignore`**, so a
workflow-only change cannot trigger a run — use `gh workflow run deploy.yml` (the `workflow_dispatch`
trigger exists for exactly this).

The `Verify app is up` step waits for Tomcat to bind **8443** before calling a deploy green; systemd's
"Started" only means it forked, and a bad credential kills the JVM seconds later.

Everything committed before 2026-08-28 is **still in git history** — those DB/JWT/RabbitMQ/Redis and
Safaricom passwords are burned and need rotating at the source. `frontend/.env` is a separate matter:
`VITE_*` vars are compiled into the public JS bundle, so `VITE_GOOGLE` is not secret by construction —
restrict that key by HTTP referrer in Google Cloud Console rather than trying to hide it.

## See also

- [frontend/CLAUDE.md](frontend/CLAUDE.md) — the React/Vite app (routing, Redux, axios tenant params, Vite 8).
- [WALLET_DEPLOYMENT.md](WALLET_DEPLOYMENT.md) — wallet/M-PESA runbook + the manual DB steps prod needs.
