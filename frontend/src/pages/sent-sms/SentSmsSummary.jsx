import React, { useMemo } from "react";
import { Skeleton } from "antd";
import MaterialIcon from "material-icons-react";
import { numberWithCommas } from "../../utils";

// Bucket the server's per-status summary into the four headline metrics.
// Order matters: pending/failed are checked before "delivered" so values like
// PENDING_DELIVERY or DeliveryImpossible aren't mistaken for a delivery.
const bucketOf = (status = "") => {
  const k = String(status).toUpperCase().trim();
  // In-progress / queued (PENDING_CREDIT, PENDING_DELIVERY, PENDING_PROCESSING, …)
  if (
    k.includes("PEND") ||
    k.includes("QUEUE") ||
    k.includes("SUBMIT") ||
    k.includes("PROCESS")
  )
    return "pending";
  // Non-delivery / errors (FAILED, ERROR, AbsentSubscriber, DeliveryImpossible,
  // InvalidMsisdn, Network Failure, SenderName Blacklisted, RS_CREDIT_ISSUE, …)
  if (
    k.includes("FAIL") ||
    k.includes("INVALID") ||
    k.includes("IMPOSSIBLE") ||
    k.includes("REJECT") ||
    k.includes("EXPIRE") ||
    k.includes("UNDELIV") ||
    k.includes("EXCEPTION") ||
    k.includes("ERROR") ||
    k.includes("ABSENT") ||
    k.includes("BLACKLIST") ||
    k.includes("CREDIT_ISSUE") ||
    k.includes("OUTCRED")
  )
    return "failed";
  // Successful delivery (DeliveredToTerminal, Success)
  if (k.includes("DELIVER") || k.includes("SUCCESS")) return "delivered";
  if (k.includes("SENT")) return "sent";
  return "other";
};

function SentSmsSummary({ summary, loading, period = "Today", onCardClick, activeKey }) {
  // Also collect the raw msg_status strings feeding each bucket, so a card click
  // can filter the (server-paginated) table by exactly those statuses.
  const { stats, statuses } = useMemo(() => {
    // Report endpoint returns an array of { msgStatus, noOfMessages, credit }.
    const rows = Array.isArray(summary) ? summary : summary?.ststusSummary || [];
    const acc = { total: 0, delivered: 0, sent: 0, failed: 0, pending: 0, cost: 0 };
    const byBucket = { delivered: new Set(), sent: new Set(), failed: new Set(), pending: new Set() };
    rows.forEach((r) => {
      const count = Number(r?.noOfMessages ?? r?.msgCount ?? 0);
      acc.total += count;
      acc.cost += Number(r?.credit ?? 0);
      const b = bucketOf(r?.msgStatus);
      if (b !== "other") {
        acc[b] += count;
        if (r?.msgStatus != null) byBucket[b]?.add(r.msgStatus);
      }
    });
    // fold pending into sent for a clean 4-card strip; keep failed distinct
    acc.sent += acc.pending;
    acc.rate = acc.total ? Math.round((acc.delivered / acc.total) * 100) : 0;
    return {
      stats: acc,
      statuses: {
        total: null, // null = no status filter (show all)
        delivered: [...byBucket.delivered],
        sent: [...byBucket.sent, ...byBucket.pending],
        failed: [...byBucket.failed],
      },
    };
  }, [summary]);

  const cards = [
    {
      key: "total",
      label: "Total Messages",
      value: stats.total,
      icon: "sms",
      color: "#69472E",
      tint: "rgba(105,71,46,0.10)",
      hint: "All sent in view",
    },
    {
      key: "delivered",
      label: "Delivered",
      value: stats.delivered,
      icon: "check_circle",
      color: "#047857",
      tint: "rgba(4,120,87,0.10)",
      hint: `${stats.rate}% delivery rate`,
    },
    {
      key: "sent",
      label: "Sent / In transit",
      value: stats.sent,
      icon: "send",
      color: "#1d4ed8",
      tint: "rgba(29,78,216,0.10)",
      hint: "Awaiting delivery report",
    },
    {
      key: "failed",
      label: "Failed",
      value: stats.failed,
      icon: "error",
      color: "#b91c1c",
      tint: "rgba(185,28,28,0.10)",
      hint: "Rejected or undelivered",
    },
  ];

  if (loading) {
    return (
      <div className="grid gap-5 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 mt-5">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="card">
            <Skeleton active paragraph={{ rows: 1 }} title={{ width: "40%" }} />
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="mt-5">
      <div className="flex items-center justify-between mb-3">
        <p className="text-[11px] font-semibold uppercase tracking-wider text-muted">
          Summary
        </p>
        <span className="badge-neutral">{period}</span>
      </div>
      <div className="grid gap-5 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4">
        {cards.map((c) => {
          const isActive = activeKey === c.key;
          return (
          <button
            type="button"
            key={c.key}
            onClick={() => onCardClick?.(c.key, statuses[c.key])}
            aria-pressed={isActive}
            className={`card card-hover flex flex-col gap-4 relative overflow-hidden text-left cursor-pointer transition-shadow focus:outline-none ${
              isActive ? "ring-2 ring-offset-1" : ""
            }`}
            style={isActive ? { "--tw-ring-color": c.color } : undefined}
          >
          <span
            className="absolute left-0 top-0 h-full w-1"
            style={{ background: c.color }}
          />
          <div className="flex items-start justify-between">
            <div
              className="h-12 w-12 rounded-xl flex items-center justify-center"
              style={{ background: c.tint }}
            >
              <MaterialIcon size={24} color={c.color} icon={c.icon} />
            </div>
            {isActive && (
              <MaterialIcon size={18} color={c.color} icon="filter_alt" />
            )}
          </div>
          <div>
            <p className="text-[11px] font-medium uppercase tracking-wider text-muted">
              {c.label}
            </p>
            <p className="text-[2rem] font-bold leading-none text-primary mt-1.5">
              {numberWithCommas(c.value || 0)}
            </p>
            <p className="text-xs text-muted mt-1.5">
              {isActive ? "Click to clear filter" : c.hint}
            </p>
          </div>
          </button>
          );
        })}
      </div>
    </div>
  );
}

export default SentSmsSummary;
