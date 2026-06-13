import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Skeleton } from "antd";
import MaterialIcon from "material-icons-react";
import { fetchTopSummary } from "../../features/billing/billingSlice";
import { cashConverter, numberWithCommas } from "../../utils";

// Platform (TOP) money + derived units view. TOP units are an untracked, infinite source, so these
// are read-only ledger derivations — cash held now, cash collected from selling units, units sold.
function TopSummaryCards() {
  const dispatch = useDispatch();
  const { topSummary, topSummaryLoading } = useSelector((state) => state.billing);

  useEffect(() => {
    dispatch(fetchTopSummary());
  }, [dispatch]);

  const cards = [
    {
      label: "Platform Cash Balance",
      value: cashConverter(topSummary?.topCashBalance),
      hint: "Available in the TOP wallet",
      icon: "account_balance_wallet",
      color: "#047857",
      tint: "rgba(4,120,87,0.10)",
    },
    {
      label: "Cash Collected (Units)",
      value: cashConverter(topSummary?.totalCashCollected),
      hint: "Total paid by resellers for units",
      icon: "payments",
      color: "#1d4ed8",
      tint: "rgba(29,78,216,0.10)",
    },
    {
      label: "Units Sold",
      value: numberWithCommas(topSummary?.totalUnitsSold),
      hint: "Units issued to resellers (no cap)",
      icon: "sms",
      color: "#69472E",
      tint: "rgba(105,71,46,0.10)",
    },
  ];

  return (
    <div className="mb-3">
      {topSummaryLoading && !topSummary ? (
        <Skeleton active paragraph={{ rows: 2 }} />
      ) : (
        <div className="grid gap-3 grid-cols-2 lg:grid-cols-3">
          {cards.map((c, i) => (
            <div
              key={i}
              className="card card-hover !p-4 flex items-start gap-3 group relative overflow-hidden"
            >
              <span
                className="absolute left-0 top-0 h-full w-1"
                style={{ background: c.color }}
              />
              <div
                className="h-9 w-9 shrink-0 rounded-lg flex items-center justify-center"
                style={{ background: c.tint }}
              >
                <MaterialIcon icon={c.icon} color={c.color} size={18} />
              </div>
              <div className="min-w-0">
                <p className="text-[10px] font-medium uppercase tracking-wider text-muted">
                  {c.label}
                </p>
                <p className="text-xl font-bold leading-none text-primary mt-1">
                  {c.value}
                </p>
                <p className="text-[11px] text-muted mt-1.5 leading-snug">{c.hint}</p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default TopSummaryCards;
