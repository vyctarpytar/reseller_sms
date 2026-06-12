import { Tabs } from "antd";
import React, { useState } from "react";
import InsideHeader from "../../components/InsideHeader";
import CodesView from "./CodesView";

function ResellerCodesView() {
  const [active, setactive] = useState("1");

  async function refetching(activeKey) {
    //   await dispatch(setRefetch());
    //   await dispatch(setRefetchKey(activeKey));
    await setactive(activeKey);
  }
  const { TabPane } = Tabs;

  const tabConfigurations = [
    {
      label: (
        <span className="flex items-center">
          <span className=" ">Details</span>
        </span>
      ),
      key: "1",
      children: <CodesView />,
    },

    {
      label: (
        <span className="flex items-center">
          <span className="">Messages</span>
        </span>
      ),
      key: "2",
      children: "Progress",
    },
  ];

  return (
    <div className="w-full h-full overflow-y-scroll">
      <InsideHeader
        title="Request Account view"
        subtitle="Manage your account and request here"
        back={true}
      />
      <div className="lg:px-10 px-3 mt-[30px] h-full">
        <div className="prod_view_title">SMS Sender ID</div>
        <div className="h-[111px]   mt-[1rem]">
          <Tabs
            items={tabConfigurations}
            defaultActiveKey="1"
            activeKey={active}
            onChange={refetching}
            className="prod-view-tab  activity-tab-wrap activity-bar"
          />
        </div>{" "}
      </div>
    </div>
  );
}

export default ResellerCodesView;
