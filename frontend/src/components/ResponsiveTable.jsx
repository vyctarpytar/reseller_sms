import React from "react";
import { Table, Pagination, Skeleton } from "antd";
import useIsMobile from "../custom_hooks/useIsMobile";

/**
 * Mark a column to drop on phones (used by the column-hiding fallback — tables
 * that don't supply a `mobileCard`). antd shows a column only when one of the
 * listed breakpoints is active, so `responsive: ['lg']` means ">= 992px only".
 */
export function hideBelow(bp = "lg") {
  return { responsive: [bp] };
}

function resolveKey(rowKey, record, index) {
  if (typeof rowKey === "function") return rowKey(record);
  if (typeof rowKey === "string" && record) return record[rowKey];
  return index;
}

/**
 * Mobile rendering: the table's rows become a vertical stack of summary cards
 * (the nineyard-capital "loan book" pattern). Each card is produced by the
 * caller's `renderCard(record, index)`; pagination is re-wired from the same
 * antd pagination config the desktop table uses.
 */
function MobileCardList({ dataSource, rowKey, loading, pagination, renderCard, emptyText }) {
  const rows = Array.isArray(dataSource) ? dataSource : [];

  if (loading) {
    return (
      <div className="space-y-2.5">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="card !p-4">
            <Skeleton active paragraph={{ rows: 1 }} title={{ width: "45%" }} />
          </div>
        ))}
      </div>
    );
  }

  if (!rows.length) {
    return (
      <div className="card !p-8 text-center text-muted text-sm">
        {emptyText || "No records found"}
      </div>
    );
  }

  const pageSize = pagination?.pageSize ?? 10;
  const total = pagination?.total ?? rows.length;
  const showPager =
    pagination &&
    pagination !== false &&
    typeof pagination.onChange === "function" &&
    total > pageSize;

  return (
    <div>
      <div className="space-y-2.5">
        {rows.map((record, index) => (
          <React.Fragment key={resolveKey(rowKey, record, index)}>
            {renderCard(record, index)}
          </React.Fragment>
        ))}
      </div>
      {showPager && (
        <div className="flex justify-center mt-4">
          <Pagination
            size="small"
            current={pagination.current}
            total={total}
            pageSize={pageSize}
            onChange={pagination.onChange}
            showSizeChanger={false}
          />
        </div>
      )}
    </div>
  );
}

/**
 * Drop-in replacement for antd <Table> with two mobile strategies, neither of
 * which touches the desktop (>= 1024px) render:
 *
 *  • If `mobileCard` is supplied, phones get a stacked card list built from it
 *    (rich, mobile-native — preferred for the main data tables).
 *  • Otherwise phones fall back to the table with a fitted `scroll.x` and any
 *    `hideBelow()` columns dropped.
 *
 * Desktop always renders the plain antd Table with every prop forwarded
 * verbatim, so the web look is unchanged.
 */
export default function ResponsiveTable({
  scroll,
  mobileCard,
  mobileEmptyText,
  dataSource,
  rowKey,
  loading,
  pagination,
  ...rest
}) {
  const isMobile = useIsMobile();

  if (isMobile && mobileCard) {
    return (
      <MobileCardList
        dataSource={dataSource}
        rowKey={rowKey}
        loading={loading}
        pagination={pagination}
        renderCard={mobileCard}
        emptyText={mobileEmptyText}
      />
    );
  }

  const effectiveScroll = isMobile ? { ...scroll, x: "max-content" } : scroll;
  return (
    <Table
      scroll={effectiveScroll}
      dataSource={dataSource}
      rowKey={rowKey}
      loading={loading}
      pagination={pagination}
      {...rest}
    />
  );
}
