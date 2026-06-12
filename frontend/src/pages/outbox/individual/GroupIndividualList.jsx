import React, { useState } from "react";
import InsideHeader from "../../../components/InsideHeader";
import { Table } from "antd";
import IndividualTable from "./IndividualTable";

function GroupIndividualList() {
  return (
    <>
      <div className="w-full overflow-y-scroll h-full">
        <InsideHeader
          title="Individual contacts"
          subtitle="Select contacts to send sms"
          back={true}
        />
        <div className="lg:px-10 px-3">
          <div className="mt-10 flex flex-col">
            <div className="product_request_title !text-[31px]">
              Send custom messages
            </div>
            <div className="product_sub  mt-[0.5rem]">
              First select contacts you want to sms by checking the table.
            </div>
          </div>

          <div>
            <IndividualTable />
          </div>
        </div>
      </div>
    </>
  );
}

export default GroupIndividualList;
