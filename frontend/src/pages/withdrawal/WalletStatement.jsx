import { Select, Tag } from "antd";
import React, { useEffect, useMemo, useState } from "react";
import ResponsiveTable, { hideBelow } from "../../components/ResponsiveTable";
import { useDispatch, useSelector } from "react-redux";
import { fetchWalletStatement } from "../../features/billing/billingSlice";
import { fetchReseller } from "../../features/reseller/resellerSlice";
import { fetchTopResellerAccounts } from "../../features/reseller-account/resellerAccountSlice";
import { cashConverter, dateForHumans, numberWithCommas } from "../../utils";

/**
 * Hierarchy-aware double-entry statement of money (KSH) + units (UNIT) movements from purchases.
 * TOP sees the whole platform and can filter by reseller, then by account under that reseller, and by
 * value type. A reseller is locked server-side to their own ledger. Each row is tagged with the owner
 * it affected. Balance is the running balance after the row (— where a unit balance isn't tracked).
 */
function WalletStatement() {
  const dispatch = useDispatch();
  const { statementData, statementCount, statementLoading } = useSelector(
    (state) => state.billing
  );
  const { resellerData } = useSelector((state) => state.reseller);
  const { topResellerAccountData } = useSelector((state) => state.resellerAccount);
  const { user } = useSelector((state) => state.auth);
  const isTop = user?.layer === "TOP";

  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [resellerId, setResellerId] = useState(null);
  const [accountId, setAccountId] = useState(null);
  const [valueType, setValueType] = useState("KSH");

  function load(overrides = {}) {
    dispatch(
      fetchWalletStatement({
        start: overrides.start ?? pageIndex,
        limit: overrides.limit ?? pageSize,
        reseller_id: overrides.reseller_id ?? resellerId,
        account_id: overrides.account_id ?? accountId,
        value_type: overrides.value_type ?? valueType,
      })
    );
  }

  useEffect(() => {
    load({ start: 0 });
    if (isTop) dispatch(fetchReseller());
  }, []);

  // When the reseller filter changes, reload the account options and clear the account filter.
  function onResellerChange(value) {
    const next = value || null;
    setResellerId(next);
    setAccountId(null);
    setPageIndex(0);
    if (next) dispatch(fetchTopResellerAccounts({ resellerId: next }));
    load({ start: 0, reseller_id: next, account_id: null });
  }

  function onAccountChange(value) {
    const next = value || null;
    setAccountId(next);
    setPageIndex(0);
    load({ start: 0, account_id: next });
  }

  function onTypeChange(value) {
    const next = value || null;
    setValueType(next);
    setPageIndex(0);
    load({ start: 0, value_type: next });
  }

  const resellerOptions = useMemo(
    () =>
      (resellerData || []).map((r) => ({
        value: r?.rsId,
        label: r?.rsCompanyName || r?.rsId,
      })),
    [resellerData]
  );

  const accountOptions = useMemo(
    () =>
      (topResellerAccountData || []).map((a) => ({
        value: a?.accId,
        label: a?.accName || a?.accId,
      })),
    [topResellerAccountData]
  );

  const isUnit = (row) => row?.valueType === "UNIT";

  const ownerTagColor = (t) =>
    t === "TOP"
      ? "!bg-[#EEF2FF] !text-[#3949AB]"
      : t === "ACCOUNT"
      ? "!bg-[#FFF4E5] !text-[#B26A00]"
      : "!bg-[#E8F5E9] !text-[#2E7D32]";

  const columns = [
    {
      title: "Date",
      dataIndex: "createdAt",
      render: (v) => <div className="whitespace-nowrap">{dateForHumans(v)}</div>,
    },
    {
      title: "Owner",
      dataIndex: "ownerName",
      ...hideBelow(),
      render: (name, row) => (
        <div className="flex flex-col gap-0.5">
          <span className="text-[#222] font-[600] whitespace-nowrap">
            {name || "—"}
          </span>
          {row?.ownerType && (
            <Tag className={`!border-0 !rounded-[6px] w-fit !text-[10px] ${ownerTagColor(row.ownerType)}`}>
              {row.ownerType}
            </Tag>
          )}
        </div>
      ),
    },
    {
      title: "Account",
      dataIndex: "accountName",
      ...hideBelow(),
      render: (v) => <div className="text-[#555] whitespace-nowrap">{v || "—"}</div>,
    },
    {
      title: "Type",
      dataIndex: "valueType",
      ...hideBelow(),
      render: (v) => (
        <Tag
          className={`!border-0 !rounded-[6px] ${
            v === "UNIT"
              ? "!bg-[#F3E8FF] !text-[#6B21A8]"
              : "!bg-[#E6F4FF] !text-[#0958D9]"
          }`}
        >
          {v === "UNIT" ? "Units" : "KSH"}
        </Tag>
      ),
    },
    {
      title: "Movement",
      dataIndex: "txLabel",
      render: (label, row) => (
        <Tag
          className={`!border-0 !rounded-[6px] ${
            row?.direction === "DEBIT"
              ? "!bg-[#FDECEC] !text-[#C0392B]"
              : "!bg-[#EAF6EC] !text-[#2A662C]"
          }`}
        >
          {label || row?.txType}
        </Tag>
      ),
    },
    {
      title: "Description",
      dataIndex: "narration",
      ...hideBelow(),
      render: (v) => <div className="text-[#555] max-w-[240px]">{v || "—"}</div>,
    },
    {
      title: "Amount",
      dataIndex: "amount",
      align: "right",
      render: (amt, row) => {
        const debit = row?.direction === "DEBIT";
        const abs = Math.abs(Number(amt) || 0);
        return (
          <div
            className={`font-[600] whitespace-nowrap ${
              debit ? "text-[#C0392B]" : "text-[#2A662C]"
            }`}
          >
            {debit ? "−" : "+"}
            {isUnit(row) ? `${numberWithCommas(abs)} u` : cashConverter(abs)}
          </div>
        );
      },
    },
    {
      title: "Balance",
      dataIndex: "balanceAfter",
      align: "right",
      render: (v, row) => {
        if (v == null) return <div className="text-[#999]">—</div>;
        const n = Number(v) || 0;
        return (
          <div className="font-[600] text-[#222] whitespace-nowrap">
            {isUnit(row) ? `${numberWithCommas(n)} u` : cashConverter(n)}
          </div>
        );
      },
    },
  ];

  return (
    <div className="mt-2">
      <div className="product_sub !text-[18px]">Wallet Statement</div>
      <div className="text-[13px] text-[#777] mt-1">
        Money and units movements from purchases — each leg tagged with the wallet/owner it affected.
      </div>

      <div className="flex flex-wrap items-center gap-3 mt-3">
        {isTop && (
          <Select
            allowClear
            showSearch
            optionFilterProp="label"
            placeholder="All resellers"
            className="min-w-[220px]"
            value={resellerId}
            onChange={onResellerChange}
            options={resellerOptions}
          />
        )}
        {isTop && (
          <Select
            allowClear
            showSearch
            optionFilterProp="label"
            placeholder={resellerId ? "All accounts" : "Select a reseller first"}
            className="min-w-[220px]"
            disabled={!resellerId}
            value={accountId}
            onChange={onAccountChange}
            options={accountOptions}
          />
        )}
        <Select
          allowClear
          placeholder="All types"
          className="min-w-[140px]"
          value={valueType}
          onChange={onTypeChange}
          options={[
            { value: "KSH", label: "KSH (money)" },
            { value: "UNIT", label: "Units" },
          ]}
        />
      </div>

      <ResponsiveTable
        dataSource={statementData}
        columns={columns}
        loading={statementLoading}
        className="mt-[.81rem] mb-[10rem] w-full"
        scroll={{ x: 1100 }}
        pagination={{
          position: ["bottomCenter"],
          current: pageIndex + 1,
          total: statementCount,
          pageSize: pageSize,
          onChange: (page, size) => {
            setPageIndex(page - 1);
            setPageSize(size);
            load({ start: page - 1, limit: size });
          },
          showSizeChanger: false,
          hideOnSinglePage: true,
        }}
        rowKey={(record) => record?.txId}
      />
    </div>
  );
}

export default WalletStatement;

