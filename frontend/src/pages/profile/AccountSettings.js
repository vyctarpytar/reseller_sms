import { useNavigate } from "react-router-dom";
import ProfileHeader from "./ProfileHeader";
import ProfileSummary from "./ProfileSummary";
import { Switch, message } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { save } from "../../features/save/saveSlice";
import { addSpaces } from "../../utils";
import InsideHeader from "../../components/InsideHeader";
import PasswordManagement from "../password-management/PasswordManagement";

export default function AccountSettings() {
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const { user } = useSelector((state) => state.auth);
  console.log("user", user);

  async function handleGetPhoneOpt() {
    await navigate("/account-settings/new-phone");
  }

  async function handleGetEmailOpt() {
    await navigate("/account-settings/new-email");
  }

  async function handleUpdatePassword() {
    await navigate("/account-settings/current-password");
  }

  return (
    <div className="w-full flex flex-col h-full overflow-y-scroll">
      <InsideHeader
        title="Account Settings"
        subtitle="Manage your account settings here"
        back={false}
      />
      <div className="w-full bg-white min-h-auto pb-[10rem]">
        <ProfileHeader />
        <div className="mt-[1.12rem]"> 
        </div>

        <div className="mt-[0px]  flex items-center justify-center w-full">
          <PasswordManagement />
        </div>
      </div>
    </div>
  );
}
