import React from "react";
import AccountAdminTable from "./AccountAdminTable";
import InsideHeader from "../../components/InsideHeader";

function AccountAdminList() {
  return (
    <div className="w-full h-full overflow-y-scroll">
      <InsideHeader
        title="Admin Portal"
        subtitle="Manage and Deactivate all accounts here"
        back={false}
      />
      <AccountAdminTable />
    </div>
  );
}

export default AccountAdminList;
