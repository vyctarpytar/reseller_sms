import React from "react";

// Maps any status string to a pill class from the design-system badge set.
// Usage: <StatusBadge value={item?.msgStatus} />
const classFor = (status = "") => {
  const k = String(status).toUpperCase();
  if (!k) return "badge-neutral";
  if (
    k.includes("DELIVER") ||
    k.includes("ACTIVE") ||
    k.includes("SUCCESS") ||
    k.includes("APPROV") ||
    k.includes("PAID") ||
    k.includes("COMPLETE") ||
    k.includes("ACCEPT")
  )
    return "badge-approved";
  if (
    k.includes("FAIL") ||
    k.includes("REJECT") ||
    k.includes("EXPIRE") ||
    k.includes("INVALID") ||
    k.includes("DELETED") ||
    k.includes("SUSPEND") ||
    k.includes("IMPOSSIBLE") ||
    k.includes("DECLINE") ||
    k.includes("CANCEL") ||
    k.includes("OUT_OF_CREDIT")
  )
    return "badge-rejected";
  if (
    k.includes("PEND") ||
    k.includes("QUEUE") ||
    k.includes("SUBMIT") ||
    k.includes("PROCESS") ||
    k.includes("WAIT") ||
    k.includes("EXCEPTION")
  )
    return "badge-pending";
  if (k.includes("SENT") || k.includes("NEW") || k.includes("OPEN") || k.includes("PROGRESS"))
    return "badge-open";
  return "badge-neutral";
};

function StatusBadge({ value, className = "" }) {
  if (value === null || value === undefined || value === "") return <span className="text-muted">—</span>;
  return <span className={`${classFor(value)} ${className}`}>{String(value)}</span>;
}

export default StatusBadge;
