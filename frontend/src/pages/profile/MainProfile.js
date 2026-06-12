import { Link } from "react-router-dom";
import ProfileHeader from "./ProfileHeader";
import ProfileSummary from "./ProfileSummary";
import { Tabs } from "antd";
import IdentityDocsTab from "./tabs/IdentityDocsTab";
import { useEffect, useState } from "react";
import OrganizationsTab from "./tabs/OrganizationsTab";
import InsideHeader from "../../components/InsideHeader";
import { fetchMyUsers } from "../../features/auth/authSlice";
import { useDispatch, useSelector } from "react-redux";

export default function MainProfile() {
  const [active, setactive] = useState(1);
 

  return (
    <div className="w-full flex flex-col h-full overflow-y-scroll">
      <InsideHeader
        title="My Profile"
        subtitle="Manage your profile details here"
        back={false}
      />
      <div className="w-full bg-white min-h-auto pb-[10rem]">
        <ProfileHeader />
        <div className="mt-[1.12rem]">
          <ProfileSummary />
        </div>
      </div>
    </div>
  );
}
