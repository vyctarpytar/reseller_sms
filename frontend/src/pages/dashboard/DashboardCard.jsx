import React from "react";
import MaterialIcon from "material-icons-react";
import { numberWithCommas } from "../../utils";

// Map an SMS status to an accent palette + glyph so each stat card reads at a glance.
const statusStyle = (status = "") => {
  const k = String(status).toUpperCase();
  if (k.includes("DELIVER") || k.includes("SUCCESS"))
    return { color: "#047857", tint: "rgba(4,120,87,0.12)", badge: "badge-approved", icon: "check_circle" };
  if (k.includes("FAIL") || k.includes("REJECT") || k.includes("EXPIRE") || k.includes("UNDELIV") || k.includes("INVALID") || k.includes("ABSENT") || k.includes("IMPOSSIBLE"))
    return { color: "#b91c1c", tint: "rgba(185,28,28,0.12)", badge: "badge-rejected", icon: "cancel" };
  if (k.includes("PEND") || k.includes("QUEUE") || k.includes("SUBMIT") || k.includes("PROCESS"))
    return { color: "#b45309", tint: "rgba(180,83,9,0.12)", badge: "badge-pending", icon: "schedule" };
  if (k.includes("SENT"))
    return { color: "#1d4ed8", tint: "rgba(29,78,216,0.12)", badge: "badge-open", icon: "send" };
  return { color: "#69472E", tint: "rgba(105,71,46,0.12)", badge: "badge-brand", icon: "sms" };
};

function DashboardCard({ dashData }) {
  return (
    <div className="grid gap-5 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4">
      {dashData?.ststusSummary &&
        dashData?.ststusSummary?.map((item, i) => {
          const s = statusStyle(item?.msgStatus);
          return (
            <div
              key={i}
              className="card card-hover flex flex-col gap-4 group relative overflow-hidden"
            >
              {/* accent rail */}
              <span
                className="absolute left-0 top-0 h-full w-1"
                style={{ background: s.color }}
              />
              <div className="flex items-start justify-between">
                <div
                  className="h-12 w-12 rounded-xl flex items-center justify-center"
                  style={{ background: s.tint }}
                >
                  <MaterialIcon icon={s.icon} color={s.color} size={24} />
                </div>
                <span className={s.badge}>{item?.msgPerCent}%</span>
              </div>

              <div>
                <p className="text-[11px] font-medium uppercase tracking-wider text-muted">
                  {item?.msgStatus}
                </p>
                <p className="text-[2rem] font-bold leading-none text-primary mt-1.5">
                  {numberWithCommas(item?.msgCount)}
                </p>
              </div>
            </div>
          );
        })}
    </div>
  );
}

export default DashboardCard;
