# Performance tuning — heavy-load runbook

Target box: **Contabo VM, 4 vCPU, 7.8 GB RAM, no swap**, with **Postgres, RabbitMQ and the JVM all
co-located** (`spring.datasource.url` and `spring.rabbitmq.host` both point at `localhost`). Every GB and
core is shared, so the guiding principle is **bounded concurrency**: a bounded DB pool, bounded worker
pools, and hard timeouts — never let one slow dependency pin the whole box.

**No swap means there is no soft landing.** Over-committing memory here is an OOM kill, not a slowdown,
so every pool on this box is explicitly capped rather than left to grow.

**Bounded is not the same as small.** The send path is *network-latency-bound*, not CPU- or RAM-bound:
a send thread spends most of its life blocked on the carrier's socket holding no DB connection, so the
right concurrency is far higher than a core count would suggest — see §1b.

## RAM budget (≈8 GB)

| Component        | Target          | Where to set |
|------------------|-----------------|--------------|
| OS + nginx       | ~0.75 GB        | — |
| Postgres         | ~2 GB           | `postgresql.conf` (below) |
| RabbitMQ (Erlang)| ~1 GB           | `rabbitmq.conf` watermark (below) |
| **JVM (app)**    | ~1.95 GB heap   | default (¼ of physical) — see §3 |
| Headroom         | ~1.5 GB         | page cache / spikes |

⚠️ **RAM is not the throughput lever here.** A thread waiting on a Safaricom socket does not want memory —
it wants a socket and a scheduler slot. The §1b concurrency raise costs roughly **70–90 MB total**
(~24 extra thread stacks, the bounded Caffeine caches, the 8192-event async log queue, ~7 extra Postgres
backends), most of it on the Postgres side. Sizing the send pipeline is a **CPU and connection** question
on this box, not a memory one.

---

## 1. In-app changes (already committed in this repo)

- **Hikari pool 100 → 25** + tighter timeouts (`application.properties`). The pool is the hard ceiling
  on concurrent DB work; 100 backends would thrash a 4-core Postgres.
- **RabbitMQ workers 100/200 → 12/16**, container consumers 2/20 → 4/8, prefetch 20 → 10
  (`RabbitMQConfig`). The SMS workers are I/O-bound; ~16 concurrent sends fit under the 25-conn pool.
- **Tomcat threads capped at 64** + scheduler pool 1 → 4 (`application.properties`).
- **HTTP timeouts everywhere on the send path**: Airtel `RestTemplate` (was none → 5s/30s); Safaricom
  SDP OkHttp (was 600s/300s → 10s connect / 60s read / 90s call) + bounded dispatcher.
- **Hibernate JDBC batching** for bulk inserts/updates.

Deploy these by pushing to `main` as usual.

---

## 1b. Send throughput (second round — also committed)

**The measurement that drove it.** With 16 consumers on `SMART_GATE_V2_RECEIVE_SYNQ`, RabbitMQ showed
`unacked = 16` — exactly the consumer count — against a 5,156-message backlog, acking at **54 msg/s**.
Because the listener processes synchronously on its consumer thread, that is the whole story:

```
throughput = consumers ÷ per-message latency
54 msg/s   = 16        ÷ 296 ms
```

Every consumer was busy 100% of the time, so the queue was never the constraint — the pipeline was. And
each message paid **5 SELECTs, 1 INSERT, 2 UPDATEs and one remote TLS round trip**, where four of those
five SELECTs re-read the *same rows* for every message in a campaign.

**What changed:**

- **Send concurrency 8/16 → 16/32** (`sms.listener.concurrency` / `.max-concurrency`). These threads
  block on the carrier's socket and hold **no DB connection while they wait**, so they were never bounded
  by the pool the way a DB-bound worker would be.

  To back it off **without a rebuild** (the properties ship inside the jar), add to
  `/opt/apps/sms-app.env` — already loaded by systemd as an `EnvironmentFile` — and restart:
  ```
  SPRING_APPLICATION_JSON='{"sms":{"listener":{"max-concurrency":20}}}'
  ```
  A plain `SMS_LISTENER_MAX_CONCURRENCY` will **not** work: these are read with `@Value`, which resolves
  the placeholder literally and applies no relaxed binding to the hyphen.
- **Hikari 25 → 32** to match. Raising `sms.listener.max-concurrency` further means raising this too.
- **DLR listener 4/8 → 8/16** (`dlr.listener.*`). Carriers post ~1 DLR per sent SMS, and the DLR queue was
  already running at ~42/s deliver against ~46/s inbound — i.e. barely keeping up at the *old* send rate.
- **`rabbitListenerTaskExecutor` queueCapacity 200 → 0.** A `ThreadPoolExecutor` only grows past
  `corePoolSize` once its queue is **full**, so core=12/queue=200 would have parked consumers 13+ in the
  queue and never started them. Harmless while `maxConcurrentConsumers` was 8 (below core); a silent cap
  the moment it is raised above it. Both executors now derive `maxPoolSize` from the matching
  `max-concurrency` rather than carrying independently-edited literals.
- **`SendMetadataCache`** (Caffeine, 60s TTL) for the per-message account / reseller / sender-ID / creator
  lookups, plus removal of a **duplicate account read** (the cost lookup re-fetched the account row the
  caller had already resolved). Balances are *never* cached — the debit is the guarded
  `UPDATE ... WHERE acc_msg_bal >= :dedAmt` against the live row — so no staleness can affect billing.
  Failed lookups aren't cached either, so mapping a sender ID mid-campaign takes effect immediately.
- **Explicit OkHttp `ConnectionPool(64, 5min)`** on the DSDP client. HTTP/1.1 is pinned (one connection
  per in-flight send), and OkHttp's default pool retains only **5 idle** connections — past that, bursty
  load re-dials and pays a fresh TCP+TLS handshake to the carrier on the hot path.
- **Async logging** (`logback-spring.xml`) and quieter hot-path logs. Boot's default CONSOLE appender is
  synchronous, so every log call was writing on a consumer thread; successful DSDP calls no longer dump
  request+response bodies at INFO (`HttpPublisherInterceptor(quietOnSuccess=true)` — set *only* on that
  client, so M-PESA and Infobip logging is unchanged, and failures still log at warn/error everywhere).

**Measuring the result:** every successful send now logs `carrierMs=` — the carrier round trip, which is
the term that caps throughput.

```bash
# per-message carrier latency distribution over the last 5k sends
journalctl -u sms-app -n 200000 --no-pager | grep -o 'carrierMs=[0-9]*' | cut -d= -f2 \
  | sort -n | awk '{a[NR]=$1} END {print "p50="a[int(NR*.5)], "p95="a[int(NR*.95)], "n="NR}'
# achieved rate
sudo rabbitmqctl list_queues name messages messages_unacknowledged consumers
```

If `p50` is most of the per-message budget, the remaining levers are **request batching** and the
carrier's own TPS limit (§7) — not this box.

---

## 2. Database indexes — REQUIRED (manual, dev + prod)

Run [`db/performance_indexes.sql`](db/performance_indexes.sql) once per environment. It uses
`CREATE INDEX CONCURRENTLY` (no table lock, safe on live prod):

```bash
psql "host=localhost dbname=synq_africa_rds user=synqadmin" -f db/performance_indexes.sql
```

Without these, the hot reads (DLR lookups by `msg_code`, dashboards, the retry cron) full-scan the
large `message_queue_arc` table on every request.

---

## 3. JVM heap — OPTIONAL (not currently applied)

⚠️ **This drop-in has never been applied in prod** (`systemctl cat sms-app` shows no `Environment=`), and
that is a deliberate choice, not an oversight. With no `-Xmx` the JVM takes ¼ of physical RAM as its heap
— ~1.95 GB on this 7.8 GB box — which is why `free -h` shows the app resident well under 2.5 GB. Low idle
free-RAM is expected and is not worth chasing. The one real argument for applying it is
`ExitOnOutOfMemoryError`: **there is no swap on this box**, so a heap runaway is an OOM kill, and this
drop-in at least lets systemd restart the JVM instead of leaving it wedged.

The jar runs under the `sms-app` unit. To cap the heap so it can't balloon into Postgres/RabbitMQ's RAM:

```bash
sudo systemctl edit sms-app
```
Add:
```ini
[Service]
Environment="JAVA_TOOL_OPTIONS=-Xms1g -Xmx2560m -XX:MaxMetaspaceSize=256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/apps/"
```
```bash
sudo systemctl daemon-reload && sudo systemctl restart sms-app
```
(`-Xmx2560m` = 2.5 GB. `ExitOnOutOfMemoryError` lets systemd restart a wedged JVM instead of it
limping. Confirm the unit's deploy path matches `systemctl cat sms-app` — see the known `/opt/apps`
vs `/opt/app` gotcha.)

---

## 4. Postgres — `postgresql.conf` (8 GB / 4-core, shared box)

```conf
max_connections = 100               # >= Hikari pool (32) + psql/admin headroom. VERIFY this: the pool
                                    # was raised to 32 in §1b, and PG refuses connections past the cap
shared_buffers = 1280MB
effective_cache_size = 3GB
work_mem = 8MB                      # per sort/hash; keep low — many can run at once
maintenance_work_mem = 256MB
checkpoint_completion_target = 0.9
max_wal_size = 2GB
min_wal_size = 512MB
wal_compression = on
random_page_cost = 1.1             # SSD
effective_io_concurrency = 200     # SSD
default_statistics_target = 100
# synchronous_commit = off         # OPTIONAL: big write throughput win, BUT can lose the last
                                   # ~0.5s of committed txns on a hard crash. Leave ON for billing
                                   # safety unless you accept that trade-off.
```
```bash
sudo systemctl restart postgresql
```

---

## 5. RabbitMQ — `/etc/rabbitmq/rabbitmq.conf`

Cap its memory so a queue backlog can't OOM the box:
```conf
vm_memory_high_watermark.absolute = 1GB
disk_free_limit.absolute = 2GB
```
```bash
sudo systemctl restart rabbitmq-server
```

---

## 6. Post-deploy verification

```bash
# DB connections should sit well under 32 in steady state (sends hold none while awaiting the carrier)
psql -c "SELECT count(*), state FROM pg_stat_activity GROUP BY state;"
# slowest queries (needs pg_stat_statements)
psql -c "SELECT mean_exec_time, calls, left(query,80) FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 15;"
free -m                              # JVM RSS + PG + RabbitMQ should leave page-cache headroom
sudo rabbitmqctl list_queues name messages messages_unacknowledged consumers
```

---

## 7. Recommended follow-ups (not applied — need a test pass)

- **`spring.jpa.open-in-view=false`** — frees DB connections earlier (good under load) but can throw
  `LazyInitializationException` during JSON serialization. Flip it and smoke-test every screen first.
- **Rewrite `cast(msg_created_date as date)` dashboard queries** to range predicates so the new
  date indexes are used (see note at the bottom of `db/performance_indexes.sql`).
- **Batch the DSDP request — the real ceiling-breaker.** `SafaricomRestSendRequest.dataSet` is already a
  `List`, the response carries a single status for the whole request, and
  `MsgMessageQueueArcRepository.updateInitialReceiveNote` already takes a `List<msgCode>` — the shape is
  built for it, but `SafaricomRestBulkService` sends `List.of(one)`. At ~20 recipients per POST the same
  32 threads would carry roughly an order of magnitude more. Needs: a batch listener (prefetch > 1),
  grouping by (account, senderId, packageId), and **written confirmation from Safaricom** that a
  multi-item dataSet is supported and how partial failures are reported. Pair it with the item below.
- **The per-account row lock — what bites at ~200/s.** `AccountRepository.updateAccountMsgBal` updates a
  single `jsc_accounts` row inside the same transaction as the arc INSERT+flush, so a single-account
  blast serializes **every** debit on that one row: the ceiling is `1 ÷ transaction duration` per account
  no matter how many consumers are added. The fix is reserving units once per batch instead of per
  message, which falls out naturally from the batching work above.
- **Ask Safaricom for the account's actual TPS limit.** Above ~110/s the carrier, not this box, is the
  most likely next constraint, and finding it by hitting it is the expensive way.
- **Move Postgres or RabbitMQ to its own VM** — the single highest-leverage change once one box can't
  keep up; co-locating all three is the real ceiling here.
- **`pg_stat_statements`** — enable it (`shared_preload_libraries`) to see real hot queries in prod.
