import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Skeleton } from "antd";
import MaterialIcon from "material-icons-react";
import { fetchDashOverview } from "../../features/dashboard/dashboardSlice";
import { fetchTopSummary } from "../../features/billing/billingSlice";
import { cashConverter, numberWithCommas } from "../../utils";

// Unified overview tier. A TOP user at platform level sees all 8 cards (3 platform-cash + 5 census)
// in one 4-up grid; drilling into a reseller, or a reseller logging in, scopes to that reseller and
// drops the platform-only cards. The cash cards come from the TOP-only /wallet/top-summary endpoint,
// so we only fetch it for TOP-layer users — the axios 403 interceptor would log a reseller out.
function OverviewCards() {
  const dispatch = useDispatch();
  const { overview, overviewLoading } = useSelector((state) => state.dash);
  const { topSummary } = useSelector((state) => state.billing);
  const user = useSelector((state) => state.auth?.user);
  const isTopUser = user?.layer === "TOP";
  // Re-fetch when the selected org changes so a TOP drill-down into a reseller cascades.
  const selectedOrg = localStorage.getItem("selectedOrg");

  useEffect(() => {
    dispatch(fetchDashOverview());
    if (isTopUser) dispatch(fetchTopSummary());
  }, [dispatch, isTopUser, selectedOrg]);

  if (overviewLoading && !overview) {
    return (
      <div className="mb-7">
        <Skeleton active paragraph={{ rows: 2 }} />
      </div>
    );
  }
  if (!overview) return null;

  const isTop = overview?.scope === "TOP";
  const a = overview?.accounts || {};
  const s = overview?.senderIds || {};
  const r = overview?.resellers || {};

  const cards = [];

  // Platform money (TOP, platform level only) — leads the grid.
  if (isTop) {
    cards.push(
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
        icon: "local_shipping",
        color: "#69472E",
        tint: "rgba(105,71,46,0.10)",
      }
    );
  }

  if (isTop) {
    cards.push({
      label: "Resellers",
      value: numberWithCommas(r?.total ?? 0),
      icon: "storefront",
      color: "#1d4ed8",
      tint: "rgba(29,78,216,0.10)",
      breakdown: [
        { label: "Active", value: r?.active, color: "#047857" },
        { label: "Inactive", value: r?.inactive, color: "#94a3b8" },
      ],
    });
  }

  cards.push({
    label: "Accounts",
    value: numberWithCommas(a?.total ?? 0),
    icon: "business",
    color: "#69472E",
    tint: "rgba(105,71,46,0.10)",
    breakdown: [
      { label: "Active", value: a?.active, color: "#047857" },
      { label: "Inactive", value: a?.inactive, color: "#94a3b8" },
      { label: "Out of credit", value: a?.outOfCredit, color: "#b45309" },
    ],
  });

  cards.push({
    label: "Sender IDs",
    value: numberWithCommas(s?.total ?? 0),
    icon: "alternate_email",
    color: "#0f766e",
    tint: "rgba(15,118,110,0.10)",
    breakdown: [
      { label: "Promotional", value: s?.promotional, color: "#D96C3B" },
      { label: "Transactional", value: s?.transactional, color: "#1d4ed8" },
      { label: "Mapped", value: s?.mapped, color: "#047857" },
      { label: "Pending", value: s?.pending, color: "#b45309" },
    ],
  });

  cards.push({
    label: isTop ? "Reseller Wallet Balance" : "Wallet Balance",
    value: cashConverter(overview?.resellerWalletBalance),
    hint: isTop
      ? "Available cash across all resellers"
      : "Your available wallet balance",
    icon: "savings",
    color: "#0e7490",
    tint: "rgba(14,116,144,0.10)",
  });

  if (isTop) {
    cards.push({
      label: "Units in Circulation",
      value: numberWithCommas(overview?.unitsInCirculation ?? 0),
      hint: "Unconsumed units held by resellers & accounts",
      icon: "sms",
      color: "#D96C3B",
      tint: "rgba(217,108,59,0.10)",
    });
  }

  return (
    <div className="mb-7">
      {/* Compact, denser tier (4-up) so the census cards sit lighter under the cash cards. */}
      <div className="grid gap-3 grid-cols-2 lg:grid-cols-4">
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
              {c.hint && <p className="text-[11px] text-muted mt-1.5 leading-snug">{c.hint}</p>}
              {c.breakdown && (
                <div className="flex flex-wrap items-center gap-x-2.5 gap-y-1 mt-2">
                  {c.breakdown.map((b) => (
                    <span
                      key={b.label}
                      className="inline-flex items-center gap-1 text-[11px]"
                    >
                      <span
                        className="h-1.5 w-1.5 rounded-full"
                        style={{ background: b.color }}
                      />
                      <span className="text-muted">{b.label}</span>
                      <span className="font-semibold text-primary">
                        {numberWithCommas(b.value ?? 0)}
                      </span>
                    </span>
                  ))}
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default OverviewCards;
