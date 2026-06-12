import { Link, useNavigate } from "react-router-dom";
import svg19 from "../assets/svg/svg19.svg";
import svg33 from "../assets/svg/svg33.svg";
import svg34 from "../assets/svg/svg34.svg";
import { useDispatch, useSelector } from "react-redux";
import { logout } from "../features/auth/authSlice";
import { useEffect, useState } from "react";
import { Dropdown, Tooltip } from "antd";
import {
  cleanResellerId,
  handleSideMenuCollapse,
} from "../features/global/globalSlice";
import MobileDrawer from "./MobileDrawer";
import logo from "../assets/img/spa_logo.png";
import { cleanCurrent } from "../features/sms-request/smsRequestSlice";
import {
  cleanBalanceHeader,
  fetchAccountBalance,
  fetchResellerBalance,
  fetchTopBalance,
} from "../features/menu/menuSlice";
import { cashConverter, getSubdomain, numberWithCommas } from "../utils";
import syncLogo from "../assets/img/sync-logo.png";
import synctelLogo from "../assets/img/synctel-logo.jpeg";
import futuresoftLogo from "../assets/img/futuresoft-logo.png";
import HeaderCrumb from "./HeaderCrumb";
import ResellerCrumb from "./ResellerCrumb";
import CreditAddSelfModal from "../pages/credit/CreditAddSelfModal";

export default function Header() {
  const { isLoggedIn, user } = useSelector((state) => state.auth);
  const { sideMenuCollapsed } = useSelector((state) => state.global);
  const { balanceHeader } = useSelector((state) => state.menu);

  const navigate = useNavigate();
  const dispatch = useDispatch();

  const [subdomain, setSubdomain] = useState("");

  const [open, setOpen] = useState(false);

  const [isModalOpenSelf, setIsModalOpenSelf] = useState(false);
  const showModalSelf = () => {
    setIsModalOpenSelf(true);
  };

  const handleRegister = () => {
    navigate("/signup-choose-account");
  };

  const handleLogin = () => {
    navigate("/account/login");
  };
  async function clean() {
    await dispatch(cleanCurrent());
  }
  async function handleLogout(e) {
    e.preventDefault();
    await dispatch(logout());
    await dispatch(cleanBalanceHeader());
    await dispatch(cleanResellerId());
    await localStorage.clear();
    await clean();
    await navigate("/");
  }

  const [show, setShow] = useState(false);
  const handleShowMenu = () => {
    setShow(!show);
  };

  const handleSideCollapse = () => {
    dispatch(handleSideMenuCollapse(!sideMenuCollapsed));
  };

  const showDrawer = () => {
    setOpen(true);
  };
  const onClose = () => {
    setOpen(false);
  };

  const handlePassowrdChange = () => {
    navigate("/password-management");
  };

  async function fetchBalanceData() {
    if (user?.layer === "RESELLER") {
      dispatch(fetchResellerBalance());
    }
    if (user?.layer === "ACCOUNT") {
      dispatch(fetchAccountBalance());
    }
    if (user?.layer === "TOP") {
      dispatch(fetchTopBalance());
    }
  }

  useEffect(() => {
    fetchBalanceData();
  }, []);

  const items = [
    {
      label: (
        <>
          <div className="bg-white flex flex-col p-[1.25rem] min-w-[15.1875rem] h-auto ">
            <div className="flex items-start justify-start">
              <img
                className="w-[3.0625rem] h-[3.0625rem] rounded-full object-cover"
                src={
                  "https://simplyilm.com/wp-content/uploads/2017/08/temporary-profile-placeholder-1.jpg"
                }
                alt="profile avatar"
              />

              <div className="flex flex-col ml-[.88rem]">
                <span className="label_2">{user?.sub}</span>
                {user?.layer != "TOP" && (
                  <>
                    <span className="label_2">
                      {cashConverter(balanceHeader?.accBalance)}
                    </span>
                    <span className="label_2">
                      {numberWithCommas(balanceHeader?.accUnits)} Units
                    </span>
                    {(user?.layer === "RESELLER" || user?.layer === "TOP") && (
                      <span
                        className={`label_2 ${
                          balanceHeader?.rsAllocatableMsgBal < 2000
                            ? "!text-red"
                            : "inherit"
                        }`}
                      >
                        {numberWithCommas(balanceHeader?.rsAllocatableMsgBal)}{" "}
                        Allocatable
                      </span>
                    )}
                  </>
                )}
              </div>
            </div>
            <div className="profile_dropdown_card mt-[1.56rem]">
              <Link
                to="/profile"
                className="view_profile_text py-[.62rem] px-[1rem]"
              >
                View profile
              </Link>

              <Link
                to="/account-settings"
                className="flex items-center py-[.62rem] px-[1rem] mt-[.62rem]"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="16"
                  height="16"
                  viewBox="0 0 16 16"
                  fill="none"
                >
                  <path
                    d="M8.00003 5C7.40668 5 6.82667 5.17595 6.33332 5.50559C5.83997 5.83523 5.45545 6.30377 5.22839 6.85195C5.00133 7.40013 4.94192 8.00333 5.05767 8.58527C5.17343 9.16721 5.45915 9.70176 5.87871 10.1213C6.29827 10.5409 6.83281 10.8266 7.41476 10.9424C7.9967 11.0581 8.5999 10.9987 9.14808 10.7716C9.69626 10.5446 10.1648 10.1601 10.4944 9.66671C10.8241 9.17336 11 8.59334 11 8C10.9992 7.2046 10.6829 6.44202 10.1204 5.87959C9.55801 5.31716 8.79542 5.00083 8.00003 5ZM8.00003 10C7.60447 10 7.21779 9.8827 6.88889 9.66294C6.55999 9.44317 6.30364 9.13082 6.15227 8.76536C6.00089 8.39991 5.96129 7.99778 6.03846 7.60982C6.11563 7.22186 6.30611 6.86549 6.58581 6.58578C6.86552 6.30608 7.22189 6.1156 7.60985 6.03843C7.99781 5.96126 8.39994 6.00086 8.7654 6.15224C9.13085 6.30361 9.4432 6.55996 9.66297 6.88886C9.88273 7.21776 10 7.60444 10 8C10 8.53043 9.78931 9.03914 9.41424 9.41421C9.03917 9.78928 8.53046 10 8.00003 10ZM13.5 8.135C13.5025 8.045 13.5025 7.955 13.5 7.865L14.4325 6.7C14.4814 6.63883 14.5153 6.56703 14.5313 6.49039C14.5474 6.41375 14.5452 6.3344 14.525 6.25875C14.3722 5.68412 14.1435 5.1324 13.845 4.61812C13.806 4.55082 13.7517 4.49357 13.6866 4.45093C13.6215 4.40829 13.5473 4.38143 13.47 4.3725L11.9875 4.2075C11.9259 4.1425 11.8634 4.08 11.8 4.02L11.625 2.53375C11.616 2.45638 11.5891 2.38219 11.5463 2.31708C11.5036 2.25197 11.4462 2.19775 11.3788 2.15875C10.8643 1.86084 10.3126 1.63239 9.73815 1.47937C9.66245 1.45925 9.58308 1.45719 9.50643 1.47336C9.42979 1.48954 9.35802 1.5235 9.2969 1.5725L8.13503 2.5C8.04503 2.5 7.95503 2.5 7.86503 2.5L6.70003 1.56937C6.63886 1.52048 6.56706 1.48663 6.49042 1.47057C6.41378 1.4545 6.33443 1.45666 6.25878 1.47687C5.68425 1.63001 5.13256 1.85867 4.61815 2.15687C4.55085 2.19595 4.4936 2.2502 4.45096 2.3153C4.40832 2.38041 4.38146 2.45456 4.37253 2.53187L4.20753 4.01687C4.14253 4.07896 4.08003 4.14146 4.02003 4.20437L2.53378 4.375C2.45641 4.384 2.38222 4.41095 2.31711 4.4537C2.252 4.49646 2.19779 4.55383 2.15878 4.62125C1.86087 5.13573 1.63242 5.6874 1.4794 6.26187C1.45928 6.33758 1.45722 6.41695 1.47339 6.49359C1.48957 6.57024 1.52353 6.64201 1.57253 6.70312L2.50003 7.865C2.50003 7.955 2.50003 8.045 2.50003 8.135L1.5694 9.3C1.52051 9.36117 1.48666 9.43296 1.4706 9.50961C1.45453 9.58625 1.45669 9.66559 1.4769 9.74125C1.62977 10.3159 1.85845 10.8676 2.1569 11.3819C2.19598 11.4492 2.25023 11.5064 2.31533 11.5491C2.38044 11.5917 2.45459 11.6186 2.5319 11.6275L4.0144 11.7925C4.07649 11.8575 4.13899 11.92 4.2019 11.98L4.37503 13.4662C4.38403 13.5436 4.41098 13.6178 4.45373 13.6829C4.49649 13.748 4.55386 13.8022 4.62128 13.8412C5.13576 14.1392 5.68743 14.3676 6.2619 14.5206C6.33761 14.5408 6.41698 14.5428 6.49362 14.5266C6.57027 14.5105 6.64204 14.4765 6.70315 14.4275L7.86503 13.5C7.95503 13.5025 8.04503 13.5025 8.13503 13.5L9.30003 14.4325C9.3612 14.4814 9.43299 14.5152 9.50964 14.5313C9.58628 14.5474 9.66562 14.5452 9.74128 14.525C10.3159 14.3721 10.8676 14.1435 11.3819 13.845C11.4492 13.8059 11.5065 13.7517 11.5491 13.6866C11.5917 13.6215 11.6186 13.5473 11.6275 13.47L11.7925 11.9875C11.8575 11.9258 11.92 11.8633 11.98 11.8L13.4663 11.625C13.5436 11.616 13.6178 11.589 13.6829 11.5463C13.7481 11.5035 13.8023 11.4462 13.8413 11.3787C14.1392 10.8643 14.3676 10.3126 14.5207 9.73812C14.5408 9.66242 14.5428 9.58305 14.5267 9.5064C14.5105 9.42976 14.4765 9.35799 14.4275 9.29687L13.5 8.135ZM12.4938 7.72875C12.5044 7.90943 12.5044 8.09057 12.4938 8.27125C12.4863 8.39495 12.5251 8.517 12.6025 8.61375L13.4894 9.72187C13.3876 10.0453 13.2573 10.359 13.1 10.6594L11.6875 10.8194C11.5645 10.833 11.4509 10.8918 11.3688 10.9844C11.2485 11.1197 11.1203 11.2478 10.985 11.3681C10.8925 11.4503 10.8337 11.5639 10.82 11.6869L10.6632 13.0981C10.3628 13.2555 10.0491 13.3858 9.72565 13.4875L8.6169 12.6006C8.52818 12.5297 8.41797 12.4912 8.3044 12.4912H8.2744C8.09373 12.5019 7.91258 12.5019 7.7319 12.4912C7.60821 12.4838 7.48615 12.5226 7.3894 12.6L6.27815 13.4875C5.95474 13.3857 5.641 13.2554 5.34065 13.0981L5.18065 11.6875C5.167 11.5645 5.10821 11.4509 5.01565 11.3687C4.88035 11.2484 4.75221 11.1203 4.6319 10.985C4.54974 10.8924 4.43616 10.8337 4.31315 10.82L2.9019 10.6625C2.74452 10.3622 2.61422 10.0484 2.51253 9.725L3.3994 8.61625C3.47684 8.5195 3.5156 8.39745 3.50815 8.27375C3.49753 8.09307 3.49753 7.91193 3.50815 7.73125C3.5156 7.60755 3.47684 7.48549 3.3994 7.38875L2.51253 6.27812C2.61429 5.95471 2.7446 5.64097 2.9019 5.34062L4.31253 5.18062C4.43554 5.16697 4.54911 5.10818 4.63128 5.01562C4.75159 4.88032 4.87972 4.75218 5.01503 4.63187C5.10795 4.54965 5.16698 4.43582 5.18065 4.3125L5.33753 2.90187C5.63784 2.74449 5.95158 2.61419 6.27503 2.5125L7.38378 3.39937C7.48052 3.47681 7.60258 3.51557 7.72628 3.50812C7.90696 3.4975 8.0881 3.4975 8.26878 3.50812C8.39248 3.51557 8.51453 3.47681 8.61128 3.39937L9.7219 2.5125C10.0453 2.61426 10.3591 2.74457 10.6594 2.90187L10.8194 4.3125C10.8331 4.43551 10.8918 4.54908 10.9844 4.63125C11.1197 4.75155 11.2478 4.87969 11.3682 5.015C11.4503 5.10755 11.5639 5.16634 11.6869 5.18L13.0982 5.33687C13.2555 5.63719 13.3858 5.95093 13.4875 6.27437L12.6007 7.38312C12.5225 7.48068 12.4837 7.604 12.4919 7.72875H12.4938Z"
                    fill="#212121"
                  />
                </svg>
                <span className="profile_text ml-[.75rem]">
                  Account settings
                </span>
              </Link>

              <button
                onClick={handleLogout}
                className="flex items-center py-[.62rem] px-[1rem] mt-[.62rem]"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="16"
                  height="16"
                  viewBox="0 0 16 16"
                  fill="none"
                >
                  <path
                    d="M7.5 13.5C7.5 13.6326 7.44732 13.7598 7.35355 13.8536C7.25979 13.9473 7.13261 14 7 14H3C2.86739 14 2.74021 13.9473 2.64645 13.8536C2.55268 13.7598 2.5 13.6326 2.5 13.5V2.5C2.5 2.36739 2.55268 2.24021 2.64645 2.14645C2.74021 2.05268 2.86739 2 3 2H7C7.13261 2 7.25979 2.05268 7.35355 2.14645C7.44732 2.24021 7.5 2.36739 7.5 2.5C7.5 2.63261 7.44732 2.75979 7.35355 2.85355C7.25979 2.94732 7.13261 3 7 3H3.5V13H7C7.13261 13 7.25979 13.0527 7.35355 13.1464C7.44732 13.2402 7.5 13.3674 7.5 13.5ZM14.3538 7.64625L11.8538 5.14625C11.7599 5.05243 11.6327 4.99972 11.5 4.99972C11.3673 4.99972 11.2401 5.05243 11.1462 5.14625C11.0524 5.24007 10.9997 5.36732 10.9997 5.5C10.9997 5.63268 11.0524 5.75993 11.1462 5.85375L12.7931 7.5H7C6.86739 7.5 6.74021 7.55268 6.64645 7.64645C6.55268 7.74021 6.5 7.86739 6.5 8C6.5 8.13261 6.55268 8.25979 6.64645 8.35355C6.74021 8.44732 6.86739 8.5 7 8.5H12.7931L11.1462 10.1462C11.0524 10.2401 10.9997 10.3673 10.9997 10.5C10.9997 10.6327 11.0524 10.7599 11.1462 10.8538C11.2401 10.9476 11.3673 11.0003 11.5 11.0003C11.6327 11.0003 11.7599 10.9476 11.8538 10.8538L14.3538 8.35375C14.4002 8.30731 14.4371 8.25217 14.4623 8.19147C14.4874 8.13077 14.5004 8.06571 14.5004 8C14.5004 7.93429 14.4874 7.86923 14.4623 7.80853C14.4371 7.74783 14.4002 7.69269 14.3538 7.64625Z"
                    fill="#212121"
                  />
                </svg>
                <span className="profile_text ml-[.75rem]">Log out</span>
              </button>
            </div>
          </div>
        </>
      ),
      key: "0",
    },
  ];

  useEffect(() => {
    setSubdomain(getSubdomain());
  }, []);

  return (
    <>
      <MobileDrawer onClose={onClose} open={open} />
      <div
        style={{
          display: "flex",
          alignItems: "center",
          background: "#FFF",
          boxShadow: "0px 4px 4px 0px rgba(0, 0, 0, 0.05)",
        }}
        className="w-full items-center justify-between sticky top-0 z-[1000] 
        py-[.5rem] lg:px-[1.875rem] px-[10px]"
      >
        <div className="flex items-center cursor-pointer">
          <div className="lg:mr-4 mr-0">
            <img
              src={svg19}
              alt="svg19"
              onClick={handleSideCollapse}
              className="lg:block hidden"
            />
          </div>
          <span
            className={`font-bold text-[25px] ${
              subdomain === "synqafrica" || subdomain == "synqtel"
                ? "!text-syncBtn"
                : "text-darkGreen"
            }  dash-title flex items-center`}
          >
            <img
              loading="lazy"
              decoding="async"
              src={
                subdomain === "synqafrica"
                  ? syncLogo
                  : subdomain === "synqtel"
                  ? synctelLogo
                  : subdomain === "futuresoft"
                  ? futuresoftLogo
                  : syncLogo
              }
              alt="logo"
              className={`${
                subdomain === "synqtel" ? "h-[5vh]" : "h-[7vh]"
              } object-contain`}
            />
            {user?.layer === "TOP" && subdomain === "synqafrica"
              ? "Synq Africa"
              : balanceHeader?.accName}
          </span>
          {user?.layer === "TOP" && (
            <div className="flex items-center ml-10">
              <HeaderCrumb />
            </div>
          )}
          {user?.layer === "RESELLER" && (
            <div className="flex items-center ml-10">
              <ResellerCrumb />
            </div>
          )}
        </div>

        {user?.layer != "TOP" && (
          <div className="lg:flex hidden items-center gap-x-3">
            {user?.layer != "RESELLER" && (
              <>
                {(() => {
                  const out = balanceHeader?.accStatus === "OUT_OF_CREDIT";
                  return (
                    <>
                      <div
                        className={`flex flex-col rounded-xl border bg-white px-4 py-1.5 ${
                          out ? "border-red/40" : "border-border"
                        }`}
                      >
                        <span className="text-[10px] uppercase tracking-wider text-muted leading-none">
                          Sms Balance
                        </span>
                        <span
                          className={`text-[16px] font-semibold leading-tight ${
                            out ? "text-red" : "text-primary"
                          }`}
                        >
                          {cashConverter(balanceHeader?.accBalance)}
                        </span>
                      </div>
                      <div
                        className={`flex flex-col rounded-xl border bg-white px-4 py-1.5 ${
                          out ? "border-red/40" : "border-border"
                        }`}
                      >
                        <span className="text-[10px] uppercase tracking-wider text-muted leading-none">
                          Units
                        </span>
                        <span
                          className={`text-[16px] font-semibold leading-tight ${
                            out ? "text-red" : "text-primary"
                          }`}
                        >
                          {numberWithCommas(balanceHeader?.accUnits)}
                        </span>
                      </div>
                    </>
                  );
                })()}
              </>
            )}

            {(user?.layer === "RESELLER" || user?.layer === "TOP") && (
              <div
                className={`flex flex-col rounded-xl border bg-white px-4 py-1.5 ${
                  balanceHeader?.rsAllocatableMsgBal < 2000
                    ? "border-red/40"
                    : "border-border"
                }`}
              >
                <span className="text-[10px] uppercase tracking-wider text-muted leading-none">
                  Units Balance
                </span>
                <span
                  className={`text-[16px] font-semibold leading-tight ${
                    balanceHeader?.rsAllocatableMsgBal < 2000
                      ? "text-red"
                      : "text-primary"
                  }`}
                >
                  {numberWithCommas(balanceHeader?.rsAllocatableMsgBal)}
                </span>
              </div>
            )}

            {user?.layer === "RESELLER" && (
              <Tooltip placement="bottom" title="Load Self Credit">
                <button
                  onClick={showModalSelf}
                  className="flex items-center justify-center h-[40px] w-[40px] rounded-xl bg-[#69472E] text-white text-[22px] leading-none hover:bg-[#7c5638] transition-colors"
                >
                  +
                </button>
              </Tooltip>
            )}
          </div>
        )}

        <div className="gap-x-[1.25rem] flex items-center cursor-pointer ">
          <div className="lg:flex hidden gap-x-[20px]">
            <span className="flex  items-center gap-x-3">
              <span
                className={`font-bold text-[16px] ${
                  subdomain === "synqafrica" || subdomain === "synqtel"
                    ? "text-syncBtn"
                    : "text-darkGreen"
                } dash-title`}
              >
                {user?.layer}
              </span>
              <Tooltip placement="bottom" title={balanceHeader?.accStatus}>
                {balanceHeader?.accStatus === "OUT_OF_CREDIT" ? (
                  <img src={svg33} alt={svg33} />
                ) : (
                  <img src={svg34} alt={svg34} />
                )}
              </Tooltip>
            </span>
            {/* <img src={svg11} alt={svg11} />
          <img src={svg12} alt={svg12} /> */}
          </div>

          <img
            src={svg19}
            alt="svg19"
            className="lg:hidden block"
            onClick={showDrawer}
          />

          <button className="green-border-btn lg:flex !hidden !items-center !justify-center ">
            <span>Individual account</span>
          </button>

          <Dropdown
            menu={{
              items,
            }}
            trigger={["click"]}
          >
            <div className="lg:pr-[1.25rem] pr-0">
              <img
                className="w-[3.0625rem] h-[3.0625rem] rounded-full object-contain"
                src={
                  "https://simplyilm.com/wp-content/uploads/2017/08/temporary-profile-placeholder-1.jpg"
                }
                alt="profile avatar"
              />
            </div>
          </Dropdown>
        </div>
      </div>

      <CreditAddSelfModal
        isModalOpen={isModalOpenSelf}
        setIsModalOpen={setIsModalOpenSelf}
        prodd={""}
      />
    </>
  );
}
