import React, { useEffect, useState } from "react";
import InsideHeader from "../../components/InsideHeader";
import svg23 from "../../assets/svg/svg23.svg";
import svg25 from "../../assets/svg/svg25.svg";
import svg26 from "../../assets/svg/svg26.svg";
import svg27 from "../../assets/svg/svg27.svg";
import svg51 from "../../assets/svg/svg51.svg";
import svg52 from "../../assets/svg/svg52.svg";
import ResellerAddModal from "./ResellerAddModal";
import { fetchReseller } from "../../features/reseller/resellerSlice";
import { useDispatch, useSelector } from "react-redux";
import { Link, useNavigate } from "react-router-dom";
import { Dropdown, Skeleton } from "antd";
import { addSpaces, formatMoney } from "../../utils";
import { setResellerId } from "../../features/global/globalSlice";
import useModalToggle from "../../custom_hooks/useModalToggle";
import { save } from "../../features/save/saveSlice";
import ConfirmModal from "../../components/ConfirmModal";
import ResellerDeleteModal from "./ResellerDeleteModal";
import toast from "react-hot-toast";

function ResellersList() {
  const { resellerData, loading } = useSelector((state) => state.reseller);
  const { saving } = useSelector((state) => state.save);

  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };
  const [prodd, setProdd] = useState();
  const [previewUrl, setPreviewUrl] = useState("");
  const [preview, setPreview] = useState(null);

  function fetchResellerData() {
    dispatch(fetchReseller());
  }

  const handleAdd = async () => {
    await setProdd("");
    await showModal();
    await setPreviewUrl("");
    await setPreview(null);
  };

  const { open, handleOpen, handleCancel } = useModalToggle();

  const [openEnable, setOpenEnable] = useState(false);
  const [selectedItem, setSelectedItem] = useState();
  const [selectedContent, setSelectedContent] = useState();
  const handleOpenEnable = async (item) => {
    await setSelectedItem(item);
    await setSelectedContent(
      prodd?.rsStatus === "ACTIVE"
        ? "Disable"
        : prodd?.rsStatus === "SUSPENDED"
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
        url: "api/v2/res",
        rsId: prodd?.rsId,
        rsStatus: selectedItem,
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await setOpenEnable(false);
      await fetchResellerData();
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  const settingItems = [
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
            prodd?.rsStatus == "SUSPENDED"
              ? handleOpenEnable("ACTIVE")
              : handleOpenEnable("SUSPENDED")
          }
        >
          <img src={svg51} alt="svg51" />{" "}
          <span className="whitespace-nowrap">
            {prodd?.rsStatus == "SUSPENDED" ? "Enable" : "Disable"}
          </span>
        </Link>
      ),
    },
  ];

  useEffect(() => {
    fetchResellerData();
  }, []);
 
  return (
    <>
      <div className="w-full overflow-y-scroll h-full">
        <InsideHeader
          title="Resellers"
          subtitle="Create and manage your resellers ready applications."
          back={false}
        />

        <div className="lg:px-10 px-3">
          <div className="mt-10 flex flex-col">
            <div className="product_request_title !text-[31px]">Resellers</div>
            <div className="product_sub  mt-[0.5rem]">
              Use this page to create and manage your resellers.
            </div>
          </div>

          {loading ? (
            <Skeleton />
          ) : (
            <div className="mt-[1rem] flex flex-shrink flex-wrap w-full gap-y-[1rem] gap-x-[1rem]">
              {resellerData?.length > 0 &&
                resellerData?.map((item) => (
                  <div
                    onClick={() => setProdd(item)}
                    className="cursor-pointer"
                  >
                    <div
                      className="product-card  min-h-[6.125rem] lg:w-[550px] w-full"
                      // onClick={()=>handleRequest(item?.rsId)}
                    >
                      <div className="flex justify-between p-[.875rem]">
                        <div className="flex items-center gap-x-5">
                          <div className={`w-[3.875rem] h-[3.875rem] flex justify-center items-center
                          ${item?.rsStatus === "DELETED"
                                ? "bg-red"
                                : "bg-primary"
                            }  
                             rounded-full`}>
                            <img src={svg23} alt={svg23} />
                          </div>

                          <div className="flex flex-col  ">
                            <div className="reseller_card_title">
                              {item?.rsCompanyName}
                            </div>
                            <div className="reseller_card_sub_title">
                              + {addSpaces(item?.rsPhoneNumber)}
                            </div>
                            <div className="reseller_card_sub_title">
                              Unit price: {formatMoney(item?.rsSmsUnitPrice)} -
                              Balance:{formatMoney(item?.rsMsgBal)}
                            </div>
                          </div>
                        </div>

                        {item?.rsStatus != "DELETED" && (
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

                      <div className="bg-[#f4f4f5] p-[.875rem] flex flex-col  h-[4rem] justify-center">
                        <div className={`reseller_card_title !text-[16px]  ${
                            item?.rsStatus === "ACTIVE"
                              ? "!text-[var(--brand)]"
                              : item?.rsStatus === "SUSPENDED" ||
                                item?.rsStatus === "DELETED"
                              ? "!text-[#ff0000]"
                              : "!text-[#000]"
                          }`}>
                         {item?.rsStatus}
                        </div>
                        <div className="reseller_card_sub_title">
                           {item?.rsDeletedReason}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
            </div>
          )}

          <div
            className="product-card !border-dashed mt-[1.875rem] mb-[1.875rem] min-h-[6.125rem] lg:w-[550px] w-full bg-[#f4f4f5] cursor-pointer"
            onClick={handleAdd}
          >
            <div className="flex items-center gap-x-5  p-[.875rem]">
              <div className="w-[3.875rem] h-[3.875rem] flex justify-center items-center bg-primary rounded-full">
                <img src={svg25} alt={"svg25"} />
              </div>

              <div className="reseller_card_title">New Reseller</div>
            </div>
          </div>
        </div>
      </div>
      <ResellerAddModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        prodd={prodd}
        previewUrl={previewUrl}
        setPreviewUrl={setPreviewUrl}
        preview={preview}
        setPreview={setPreview}
      />

      <ResellerDeleteModal
        open={open}
        handleCancel={handleCancel}
        prodd={prodd}
        handleFetch={fetchResellerData}
      />

      <ConfirmModal
        open={openEnable}
        handleCancel={handleCloseEnable}
        handleSubmit={handleEnable}
        loading={saving}
        content={`Are you sure you want to ${selectedContent} ${prodd?.rsCompanyName}?`}
        type="info"
        btnTitle={`${selectedContent}`}
      />
    </>
  );
}

export default ResellersList;
