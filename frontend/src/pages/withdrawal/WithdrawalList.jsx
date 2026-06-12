import React from "react";
import { Tabs } from "antd";
import WithdrawalHeader from "./WithdrawalHeader";
import PayoutMethod from "./PayoutMethod";
import PaymentHistory from "./PaymentHistory";
import PayoutHistory from "./PayoutHistory";
import WalletStatement from "./WalletStatement";

function WithdrawalList() {
  const items = [
    { key: "statement", label: "Statement", children: <WalletStatement /> },
    { key: "payouts", label: "Payout History", children: <PayoutHistory /> },
    { key: "growth", label: "Growth", children: <PaymentHistory /> },
    { key: "tariff", label: "Payout Tariff", children: <PayoutMethod /> },
  ];

  return (
    <div className="w-full h-full overflow-y-scroll">
      <div className="lg:px-10 px-3">
        <div className="mt-3 product_request_title !text-[31px]">
          Balance & withdrawal
        </div>

        <WithdrawalHeader />

        <Tabs
          defaultActiveKey="statement"
          items={items}
          className="mt-8 wallet-tabs"
        />
      </div>
    </div>
  );
}

export default WithdrawalList;
