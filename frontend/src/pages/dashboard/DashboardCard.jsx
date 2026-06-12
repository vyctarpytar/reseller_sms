import React from "react";
import smsSvg from "../../assets/svg/smsDash.svg";
import { numberWithCommas } from "../../utils";

// Map an SMS status to an accent palette so each stat card reads at a glance.
const statusStyle = (status = "") => {
  const k = String(status).toUpperCase();
  if (k.includes("DELIVER"))
    return { color: "#047857", tint: "rgba(4,120,87,0.10)", badge: "badge-approved" };
  if (k.includes("FAIL") || k.includes("REJECT") || k.includes("EXPIRE") || k.includes("UNDELIV"))
    return { color: "#b91c1c", tint: "rgba(185,28,28,0.10)", badge: "badge-rejected" };
  if (k.includes("PEND") || k.includes("QUEUE") || k.includes("SUBMIT") || k.includes("PROCESS"))
    return { color: "#b45309", tint: "rgba(180,83,9,0.10)", badge: "badge-pending" };
  if (k.includes("SENT"))
    return { color: "#1d4ed8", tint: "rgba(29,78,216,0.10)", badge: "badge-open" };
  return { color: "#69472E", tint: "rgba(105,71,46,0.10)", badge: "badge-brand" };
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
                  <img src={smsSvg} alt="" className="h-6 w-6" />
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
