import React from "react";
import { Table } from "antd";
import useIsMobile from "../custom_hooks/useIsMobile";

/**
 * Mark a column to drop on phones. antd shows a column only when one of the
 * listed breakpoints is active, so `responsive: ['lg']` means ">= 992px only".
 * Spread it onto any non-essential column:  { title: 'Reseller', ...hideBelow() }
 * Essential columns get nothing and stay visible on every screen.
 */
export function hideBelow(bp = "lg") {
  return { responsive: [bp] };
}

/**
 * Drop-in replacement for antd <Table> that makes wide tables fit on phones
 * WITHOUT changing the desktop render at all.
 *
 *  • Desktop (>= 1024px): `scroll` is passed straight through — identical to a
 *    plain antd Table, so the web look is untouched.
 *  • Mobile (< 1024px): a fixed pixel `scroll.x` (e.g. {x: 2000}) is replaced
 *    with {x: 'max-content'} so the table sizes to whatever columns survive the
 *    `hideBelow` filtering, instead of forcing a horizontal pan. Any `scroll.y`
 *    is preserved.
 *
 * Everything else (columns, dataSource, pagination, rowKey, loading, className…)
 * is forwarded verbatim via {...rest}.
 */
export default function ResponsiveTable({ scroll, ...rest }) {
  const isMobile = useIsMobile();
  const effectiveScroll = isMobile ? { ...scroll, x: "max-content" } : scroll;
  return <Table scroll={effectiveScroll} {...rest} />;
}
