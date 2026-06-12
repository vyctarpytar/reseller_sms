import React from "react";
import WithdrawalHeader from "./WithdrawalHeader";
import PayoutMethod from "./PayoutMethod";
import PaymentHistory from "./PaymentHistory";
import PayoutHistory from "./PayoutHistory";

function WithdrawalList() {
  return (
    <div className="w-full h-full overflow-y-scroll">
      <div className="lg:px-10 px-3">
        <div className="mt-3 product_request_title !text-[31px]">
          Balance & withdrawal
        </div>

        <WithdrawalHeader />
        <div className="flex lg:flex-row flex-col lg:gap-x-[1rem] gap-y-[1rem] mt-8">
          <PayoutMethod />
          <PaymentHistory />
        </div>
        <PayoutHistory />
      </div>
    </div>
  );
}

export default WithdrawalList;
