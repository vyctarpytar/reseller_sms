import React, { useEffect, useState } from "react";
import InsideHeader from "../../components/InsideHeader";
import svg23 from "../../assets/svg/svg23.svg";
import svg25 from "../../assets/svg/svg25.svg";
import svg26 from "../../assets/svg/svg26.svg";
import svg27 from "../../assets/svg/svg27.svg";
import svg51 from "../../assets/svg/svg51.svg";
import svg52 from "../../assets/svg/svg52.svg";
import { useDispatch, useSelector } from "react-redux";
import { Link, useNavigate } from "react-router-dom";
import { Dropdown, Input, Skeleton } from "antd";
import { addSpaces, cashConverter, formatMoney, formatText } from "../../utils";
import AccountAddModal from "./AccountAddModal";
import { fetchResellerAccounts } from "../../features/reseller-account/resellerAccountSlice";
import { save } from "../../features/save/saveSlice";
import toast from "react-hot-toast";
import ConfirmModal from "../../components/ConfirmModal";
import AccountDeleteModal from "./AccountDeleteModal";
import useModalToggle from "../../custom_hooks/useModalToggle";
import { setTenantScope } from "../../custom_hooks/useTenantScope";

function AccountsList() {
  const { resellerAccountData, loading } = useSelector(
    (state) => state.resellerAccount
  );
  const { user } = useSelector((state) => state.auth);
  const { saving } = useSelector((state) => state.save);

  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };
  const [prodd, setProdd] = useState();
  const [searchText, setSearchText] = useState("");

  const filteredData = searchText
    ? resellerAccountData?.filter((item) =>
        item?.accName?.toLowerCase().includes(searchText.toLowerCase())
      )
    : resellerAccountData;

  function fetchResellerAccountData() {
    dispatch(fetchResellerAccounts());
  }

  const handleAdd = async () => {
    await setProdd("");
    await showModal();
  };

  const { open, handleOpen, handleCancel } = useModalToggle();

  const [openEnable, setOpenEnable] = useState(false);
  const [selectedItem, setSelectedItem] = useState();
  const [selectedContent, setSelectedContent] = useState();
  const handleOpenEnable = async (item) => {
    await setSelectedItem(item);
    await setSelectedContent(
      prodd?.accStatus === "ACTIVE"
        ? "Disable"
        : prodd?.accStatus === "SUSPENDED"
        ? "Activate"
        : null
    );
    await setOpenEnable(true);
  };
  const handleCloseEnable = () => {
    setOpenEnable(false);
  };

  const handleEnable = async () => {
    const res = await dispatch(
      save({
        url: "api/v2/account",
        accId: prodd?.accId,
        accStatus: selectedItem,
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await setOpenEnable(false);
      await fetchResellerAccountData();
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  const settingItems = [
    ...(user?.layer !== "ACCOUNT"
      ? [
          {
            key: "open",
            label: (
              <Link
                className="flex gap-x-[.75rem] items-center py-[.5rem]"
                onClick={() => handleRequest(prodd)}
              >
                <img src={svg23} alt="svg23" className="w-4 h-4" />
                <span className="whitespace-nowrap">Open account</span>
              </Link>
            ),
          },
          {
            key: "divider-open",
            type: "divider",
          },
        ]
      : []),
    {
      key: "1",
      label: (
        <Link
          className="flex gap-x-[.75rem] items-center py-[.5rem]"
          onClick={showModal}
        >
          <img src={svg26} alt="svg26" className="w-4 h-4" /> Edit
        </Link>
      ),
    },
    {
      key: "divider-1",
      type: "divider",
    },
    {
      key: "2",
      label: (
        <Link
          className="flex gap-x-[.75rem] items-center py-[.5rem]"
          onClick={() => handleOpen()}
        >
          <img src={svg52} alt="svg52" className="w-4 h-4" />
          <span className="whitespace-nowrap">Delete</span>
        </Link>
      ),
    },
    {
      key: "divider-2",
      type: "divider",
    },

    {
      key: "3",
      label: (
        <Link
          className="flex gap-x-[.75rem] items-center py-[.5rem]"
          onClick={() =>
            prodd?.accStatus == "SUSPENDED"
              ? handleOpenEnable("ACTIVE")
              : handleOpenEnable("SUSPENDED")
          }
        >
          <img src={svg51} alt="svg51" />{" "}
          <span className="whitespace-nowrap">
            {prodd?.accStatus == "SUSPENDED" ? "Enable" : "Disable"}
          </span>
        </Link>
      ),
    },
  ];

  const handleEdit = async () => {
    showModal();
  };

  // Drill into an account ("navigate as that account"): scope every
  // subsequent API call to it by setting selectedAccount, then let
  // DashboardMain route to the account-level dashboard. Mirrors the
  // TOP-level drill in HeaderCrumb.handleAccClick.
  const handleRequest = (account) => {
    if (user?.layer === "ACCOUNT") return;
    if (account?.accStatus === "DELETED") return;
    setTenantScope({ account: account?.accId, accountName: account?.accName });
    navigate("/dashboard-main");
  };

  useEffect(() => {
    fetchResellerAccountData();
  }, []);

  return (
    <>
      <div className="w-full overflow-y-scroll h-full">
        <InsideHeader
          title="Accounts"
          subtitle="Create and manage your accounts ready applications."
          back={false}
        />

        <div className="lg:px-10 px-3">
          <div className="mt-10 flex flex-col">
            <div className="product_request_title !text-[31px]">Accounts</div>
            <div className="product_sub  mt-[0.5rem] flex lg:flex-row flex-col justify-between">
              <span>Create and manage your accounts ready applications.</span>
              <div className="flex items-center lg:w-[23%] w-full justify-end">
                <Input
                  value={searchText}
                  onChange={(e) => setSearchText(e.target.value)}
                  placeholder="Search account name"
                  className="text-[16px] font-[400] flex-row-reverse"
                  prefix={
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      width="24"
                      height="24"
                      viewBox="0 0 24 24"
                      fill="none"
                    >
                      <path
                        fill-rule="evenodd"
                        clip-rule="evenodd"
                        d="M11.2508 3.75C7.50609 3.75 4.47041 6.93997 4.47041 10.875C4.47041 14.81 7.50609 18 11.2508 18C14.9955 18 18.0312 14.81 18.0312 10.875C18.0312 6.93997 14.9955 3.75 11.2508 3.75ZM3.04297 10.875C3.04297 6.11154 6.71773 2.25 11.2508 2.25C15.7838 2.25 19.4586 6.11154 19.4586 10.875C19.4586 15.6385 15.7838 19.5 11.2508 19.5C6.71773 19.5 3.04297 15.6385 3.04297 10.875Z"
                        fill="#333333"
                      />
                      <path
                        fill-rule="evenodd"
                        clip-rule="evenodd"
                        d="M16.045 15.913C16.3237 15.6201 16.7756 15.6201 17.0543 15.913L21.3902 20.4693C21.6689 20.7622 21.6689 21.237 21.3902 21.5299C21.1115 21.8228 20.6596 21.8228 20.3809 21.5299L16.045 16.9737C15.7663 16.6808 15.7663 16.2059 16.045 15.913Z"
                        fill="#333333"
                      />
                    </svg>
                  }
                />
              </div>
            </div>
          </div>

          {loading ? (
            <Skeleton />
          ) : (
            <div className="mt-[1rem] flex flex-shrink flex-wrap w-full gap-y-[1rem] gap-x-[1rem]">
              {filteredData?.length > 0 &&
                filteredData?.map((item) => (
                  <div
                    onClick={() => setProdd(item)}
                    className="cursor-pointer"
                  >
                    <div className="product-card  min-h-[7.125rem] lg:w-[550px] w-full">
                      <div className="flex justify-between p-[.875rem]">
                        <div
                          className="flex items-center gap-x-5"
                          onClick={() => handleRequest(item)}
                        >
                          <div
                            className={`w-[3.875rem] h-[3.875rem] flex justify-center items-center ${
                              item?.accMsgBal < 50
                                ? "bg-[#ffa500]"
                                : item?.accStatus === "DELETED"
                                ? "bg-red"
                                : "bg-primary"
                            }   rounded-full`}
                          >
                            <img src={svg23} alt={svg23} />
                          </div>

                          <div className="flex flex-col  ">
                            <div className="reseller_card_title">
                              {item?.accName}
                            </div>
                            <div className="reseller_card_sub_title">
                              +{addSpaces(item?.accAdminMobile)}
                            </div>
                            <div className="reseller_card_sub_title">
                              {cashConverter(item?.accSmsPrice)} - Sms Bal:{" "}
                              {cashConverter(item?.accMsgBal)}
                            </div>
                          </div>
                        </div>
                        {item?.accStatus != "DELETED" && (
                          <Dropdown
                            overlayStyle={{ width: "250px" }}
                            trigger={["click"]}
                            menu={{ items: settingItems }}
                            placement="bottom"
                          >
                            <button onClick={() => setProdd(item)}>
                              <img src={svg27} alt="svg27" />
                            </button>
                          </Dropdown>
                        )}
                      </div>

                      <div className="bg-[#f4f4f5] p-[.875rem] flex flex-col h-[4rem] justify-center">
                        <div
                          className={`reseller_card_title !text-[16px] ${
                            item?.accStatus === "ACTIVE"
                              ? "!text-[var(--brand)]"
                              : item?.accStatus === "SUSPENDED" ||
                                item?.accStatus === "DELETED"
                              ? "!text-[#ff0000]"
                              : "!text-[#000]"
                          }`}
                        >
                          {item?.accStatus}
                        </div>
                        <div className="reseller_card_sub_title">
                          {item?.accDeletedReason}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
            </div>
          )}

          {user?.layer != "ACCOUNT" && (
            <div
              className="product-card mb-10 !border-dashed mt-[1.875rem] min-h-[6.125rem] lg:w-[550px] w-full bg-[#f4f4f5] cursor-pointer"
              onClick={handleAdd}
            >
              <div className="flex items-center gap-x-5  p-[.875rem]">
                <div className="w-[3.875rem] h-[3.875rem] flex justify-center items-center bg-primary rounded-full">
                  <img src={svg25} alt={"svg25"} />
                </div>

                <div className="reseller_card_title">New Account</div>
              </div>
            </div>
          )}
        </div>
      </div>
      <AccountAddModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        prodd={prodd}
      />

      <AccountDeleteModal
        open={open}
        handleCancel={handleCancel}
        prodd={prodd}
        handleFetch={fetchResellerAccountData}
      />

      <ConfirmModal
        open={openEnable}
        handleCancel={handleCloseEnable}
        handleSubmit={handleEnable}
        loading={saving}
        content={`Are you sure you want to ${selectedContent} ${prodd?.accName}?`}
        type="info"
        btnTitle={`${selectedContent}`}
      />
    </>
  );
}

export default AccountsList;
