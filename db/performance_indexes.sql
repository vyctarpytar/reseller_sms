-- ============================================================================
-- Synq SMS — hot-path indexes for heavy load
-- ============================================================================
-- Run ONCE per environment (dev AND prod). CI has no migration step, so this is
-- manual — same as the other prod DB steps in WALLET_DEPLOYMENT.md.
--
-- HOW TO RUN (important):
--   CREATE INDEX CONCURRENTLY cannot run inside a transaction block, and it does
--   NOT lock the table for writes — safe to run on the live prod DB. Run it with
--   psql in autocommit (the default), one statement at a time:
--
--     psql "host=localhost dbname=synq_africa_rds user=synqadmin" -f db/performance_indexes.sql
--
--   If a CONCURRENTLY build is interrupted it can leave an INVALID index; find and
--   drop it, then re-run that one line:
--     SELECT indexrelid::regclass FROM pg_index WHERE NOT indisvalid;
--     DROP INDEX CONCURRENTLY <name>;
--
-- All statements are IF NOT EXISTS, so re-running is safe.
-- ============================================================================


-- ── msg.message_queue_arc — the giant, hottest table ────────────────────────

-- Delivery-report path (the highest-frequency lookup): every DLR/callback hits
-- the arc table by msg_code — findByMsgCode, updateDeliverNote (msg_code+msisdn),
-- updateInitialReceiveNote (msg_code IN (...)).
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_mqa_msg_code
    ON msg.message_queue_arc (msg_code);

-- Per-account message history + most dashboard/report queries filter by account
-- and order/scan by date.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_mqa_acc_created
    ON msg.message_queue_arc (msg_acc_id, msg_created_date DESC);

-- Status-scoped scans (getWeiserPendingDNR 'SENT', resend selectors, status counts).
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_mqa_status_created
    ON msg.message_queue_arc (msg_status, msg_created_date);

-- Retry cron (SchedulingConfig.retryFailedMessages -> findRetryBatch). Partial
-- index: only the not-yet-retried rows, which is the tiny slice the cron scans.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_mqa_retry
    ON msg.message_queue_arc (msg_created_date)
    WHERE msg_sent_retried IS NOT TRUE;

-- Client delivery-callback cron (ClientDeliveryResponses): pending callbacks
-- ordered by last attempt. Partial index keeps it small.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_mqa_pending_callbacks
    ON msg.message_queue_arc (msg_last_callback_attempt)
    WHERE msg_callback_url IS NOT NULL AND msg_client_delivery_status = 'PENDING';


-- ── js_core.jsc_accounts ────────────────────────────────────────────────────

-- Dashboards/reports scope by reseller via EXISTS(... acc_reseller_id = :rs ...),
-- and findAllByAccResellerId; also the per-send account->reseller resolution.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_jsc_accounts_reseller
    ON js_core.jsc_accounts (acc_reseller_id);


-- ── msg.airtel_numbers — grows unbounded, checked on the send path ───────────

-- existsByAnNumber(...) runs per send (AiretelService.checkIsAirtel) and again
-- after each Airtel send. This table only grows, so a seq-scan here degrades over
-- time. NOTE: if an_number already has a UNIQUE constraint (saveAirtelNumberWithRetry
-- retries on duplicate-key, which hints one exists), that constraint already indexes
-- it and you can skip this line.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_airtel_numbers_an_number
    ON msg.airtel_numbers (an_number);


-- ── js_core.operator_prefix — per-number MNO lookup ─────────────────────────

-- findByOpPrefixAndOpOperator. Small table, low cost; index for completeness.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_operator_prefix_lookup
    ON js_core.operator_prefix (op_prefix, op_operator);


-- ── Refresh planner stats so the new indexes get used immediately ───────────
ANALYZE msg.message_queue_arc;
ANALYZE js_core.jsc_accounts;
ANALYZE msg.airtel_numbers;
ANALYZE js_core.operator_prefix;

-- ----------------------------------------------------------------------------
-- FOLLOW-UP (code change, not in this script): several dashboard queries filter
-- with  cast(msg_created_date as date) = current_date , and the cast prevents any
-- btree index on msg_created_date from being used. Rewrite those to a sargable
-- range — e.g.  msg_created_date >= date_trunc('day', now())
--                AND msg_created_date <  date_trunc('day', now()) + interval '1 day'
-- so idx_mqa_acc_created / idx_mqa_status_created can serve them.
-- ----------------------------------------------------------------------------
