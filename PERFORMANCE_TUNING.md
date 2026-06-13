# Performance tuning — heavy-load runbook

Target box: **Contabo VM, 4 vCPU, 8 GB RAM**, with **Postgres, RabbitMQ and the JVM all co-located**
(`spring.datasource.url` and `spring.rabbitmq.host` both point at `localhost`). Every GB and core is
shared, so the guiding principle is **bounded concurrency**: a small DB pool, a small worker pool, and
hard timeouts — never let one slow dependency pin the whole box.

## RAM budget (≈8 GB)

| Component        | Target          | Where to set |
|------------------|-----------------|--------------|
| OS + nginx       | ~0.75 GB        | — |
| Postgres         | ~2 GB           | `postgresql.conf` (below) |
| RabbitMQ (Erlang)| ~1 GB           | `rabbitmq.conf` watermark (below) |
| **JVM (app)**    | **~2.5 GB heap**| systemd drop-in (below) |
| Headroom         | ~1.5 GB         | page cache / spikes |

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

## 2. Database indexes — REQUIRED (manual, dev + prod)

Run [`db/performance_indexes.sql`](db/performance_indexes.sql) once per environment. It uses
`CREATE INDEX CONCURRENTLY` (no table lock, safe on live prod):

```bash
psql "host=localhost dbname=synq_africa_rds user=synqadmin" -f db/performance_indexes.sql
```

Without these, the hot reads (DLR lookups by `msg_code`, dashboards, the retry cron) full-scan the
large `message_queue_arc` table on every request.

---

## 3. JVM heap — set via systemd (one-time on the VM)

The jar runs under the `sms-app` unit. Cap the heap so it can't balloon into Postgres/RabbitMQ's RAM:

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
max_connections = 100               # >= Hikari pool (25) + psql/admin headroom
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
# DB connections should sit well under 25 in steady state
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
- **Move Postgres or RabbitMQ to its own VM** — the single highest-leverage change once one box can't
  keep up; co-locating all three is the real ceiling here.
- **`pg_stat_statements`** — enable it (`shared_preload_libraries`) to see real hot queries in prod.
