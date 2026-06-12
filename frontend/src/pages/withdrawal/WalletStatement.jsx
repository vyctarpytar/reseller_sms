import { Table, Tag } from "antd";
import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { fetchWalletStatement } from "../../features/billing/billingSlice";
import { cashConverter, dateForHumans } from "../../utils";

/**
 * Full cash-wallet ledger (the only view of every wallet movement — deposits, unit-purchase
 * debits, withdrawals, reversals). Amount is signed; balanceAfter is the running balance.
 */
function WalletStatement() {
  const dispatch = useDispatch();
  const { statementData, statementCount, statementLoading } = useSelector(
    (state) => state.billing
  );

  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  function load(page, size) {
    dispatch(
      fetchWalletStatement({
        start: page ?? pageIndex,
        limit: size ?? pageSize,
      })
    );
  }

  useEffect(() => {
    load(0, pageSize);
  }, []);

  const columns = [
    {
      title: "Date",
      dataIndex: "createdAt",
      render: (v) => <div className="whitespace-nowrap">{dateForHumans(v)}</div>,
    },
    {
      title: "Type",
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
      render: (v) => <div className="text-[#555]">{v || "—"}</div>,
    },
    {
      title: "Reference",
      dataIndex: "reference",
      render: (v) => (
        <div className="text-[12px] text-[#888] break-all max-w-[220px]">
          {v || "—"}
        </div>
      ),
    },
    {
      title: "Amount",
      dataIndex: "amount",
      align: "right",
      render: (amt, row) => {
        const debit = row?.direction === "DEBIT";
        return (
          <div
            className={`font-[600] whitespace-nowrap ${
              debit ? "text-[#C0392B]" : "text-[#2A662C]"
            }`}
          >
            {debit ? "−" : "+"}
            {cashConverter(Math.abs(Number(amt) || 0))}
          </div>
        );
      },
    },
    {
      title: "Balance",
      dataIndex: "balanceAfter",
      align: "right",
      render: (v) => (
        <div className="font-[600] text-[#222] whitespace-nowrap">
          {cashConverter(Number(v) || 0)}
        </div>
      ),
    },
  ];

  return (
    <div className="mt-2">
      <div className="product_sub !text-[18px]">Wallet Statement</div>
      <div className="text-[13px] text-[#777] mt-1">
        Every movement on your wallet — deposits, unit purchases, withdrawals and
        reversals.
      </div>
      <Table
        dataSource={statementData}
        columns={columns}
        loading={statementLoading}
        className="mt-[.81rem] mb-[10rem] w-full"
        scroll={{ x: 900 }}
        pagination={{
          position: ["bottomCenter"],
          current: pageIndex + 1,
          total: statementCount,
          pageSize: pageSize,
          onChange: (page, size) => {
            setPageIndex(page - 1);
            setPageSize(size);
            load(page - 1, size);
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
