import { Badge, Dropdown, Skeleton, Spin, Table, Tooltip } from "antd";
import React, { useEffect, useRef, useState } from "react";
import InsideHeader from "../../components/InsideHeader";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import svg32 from "../../assets/svg/svg32.svg";
import MaterialIcon from "material-icons-react";
import { fetchSentSms } from "../../features/sms-request/smsRequestSlice";
import {
  addSpaces,
  cashConverter,
  dateForHumans,
  formatDate,
  formatDateTime,
} from "../../utils";
import noCon from "../../assets/img/noCon.png";
import svg38 from "../../assets/svg/svg38.svg";
import FilterModal from "./FilterModal";
import {
  downloadExcel,
  fetchSavedSms,
  save,
} from "../../features/save/saveSlice";
import toast from "react-hot-toast";

function SentSmsList() {
  const [notOpen, setnotOpen] = useState(false);
  const { loading } = useSelector((state) => state.sms);
  const { user } = useSelector((state) => state.auth);
  const { saving } = useSelector((state) => state.save);
  const { sentSmsData, loadingSms, sentSmsCount } = useSelector(
    (state) => state.save
  );

  const [formData, setFormData] = useState({});
  const handleOpenChange = () => {
    setnotOpen(false);
  };

  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };

  const truncateText = (text, maxLength) => {
    if (text?.length > maxLength) {
      return text?.substring(0, maxLength - 3) + "...";
    }
    return text;
  };

  const hasResellerName = sentSmsData?.some(
    (item) =>
      item?.msgResellerName !== null &&
      item?.msgResellerName !== undefined &&
      user?.layer === "TOP"
  );

  const columns = [
    {
      title: "Text",
      render: (item) => {
        return (
          <>
            <Tooltip title={item}>
              <div className="text-[14px]">{truncateText(item, 150)}</div>
            </Tooltip>
          </>
        );
      },
      width: "30%",
      dataIndex: "msgMessage",
    },
    ...(hasResellerName
      ? [
          {
            title: "Reseller Name",
            render: (item) => {
              return <div>{item}</div>;
            },
            width: "10%",
            dataIndex: "msgResellerName",
          },
        ]
      : []),
    {
      title: "Account Name",
      dataIndex: "msgAccName",
      width: "10%",
    },
    {
      title: "Sender Name",
      dataIndex: "msgSenderIdName",
      width: "5%",
    },
    {
      title: "Phone Number",
      render: (item) => {
        return <div>{addSpaces(item)}</div>;
      },
      width: "8%",
      dataIndex: "msgSubMobileNo",
    },
    {
      title: "Status",
      render: (item) => {
        return (
          <div
            className={`${
              item?.msgStatus == "SENT"
                ? "text-[#8884d8]"
                : item?.msgStatus == "DeliveredToTerminal"
                ? "text-[#388E3C]"
                : item?.msgStatus == "Exception sending "
                ? "text-[#ffa500]"
                : item?.msgStatus == "InvalidMsisdn"
                ? "text-[#ff0000]"
                : item?.msgStatus == "DeliveryImpossible"
                ? "text-[#808080]"
                : "text-blk14"
            }  
         font-[700] flex items-center justify-center text-center`}
          >
            {item?.msgStatus}
          </div>
        );
      },
      width: "10%",
    },
    {
      title: "Pages",
      dataIndex: "msgPage",
      width: "5%",
    },
    {
      title: "Cost",
      render: (item) => {
        return <div>{cashConverter(item)}</div>;
      },
      dataIndex: "msgCostId",
      width: "5%",
    },
    {
      title: "Date",
      // render: (item) => {
      //   return <div>{formatDateTime(item)}</div>;
      // },
      dataIndex: "msgCreatedDate",
      width: "10%",
    },
    {
      title: "Sent By",
      dataIndex: "msgCreatedByEmail",
      width: "10%",
    },
  ];

  const navigate = useNavigate();
  const dispatch = useDispatch();

  const handleSendSms = () => {
    navigate("/outbox");
  };

  const handleClearFilters = async () => {
    await setFormData({});
    const res = await dispatch(
      fetchSavedSms({
        url: "api/v2/sms",
        msgStatus: null,
        msgCreatedDate: null,
        msgSubmobileNo: null,
        msgMessage: null,
        msgAccId: null,
        msgSenderId: null,
        msgCreatedFrom: null,
        msgCreatedTo: null,
      })
    );
  };

  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  async function fetchSentSmsData(page, size) {
    const res = await dispatch(
      fetchSavedSms({
        url: "api/v2/sms",
        limit: size ?? pageSize,
        start: page ?? pageIndex,
        msgStatus: formData?.msgStatus,
        msgCreatedFrom: formData?.msgCreatedFrom,
        msgCreatedTo: formData?.msgCreatedTo,
        msgSubmobileNo: formData?.msgSubmobileNo,
        msgMessage: formData?.msgMessage,
        msgAccId: formData?.msgAccId,
        msgSenderId: formData?.msgSenderId,
      })
    );
  }

  const handleClick = async (item) => {
    const res = await dispatch(
      downloadExcel({
        url: "api/v2/sms/download-excel",
        msgStatus: formData?.msgStatus,
        msgCreatedFrom: formData?.msgCreatedFrom,
        msgCreatedTo: formData?.msgCreatedTo,
        msgSubmobileNo: formData?.msgSubmobileNo,
        msgMessage: formData?.msgMessage,
        msgAccId: formData?.msgAccId,
        msgSenderId: formData?.msgSenderId,
      })
    );

    if (res?.payload) {
      const blob = new Blob([res.payload], {
        type: "application/octet-stream",
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", "sent-sms-list.xlsx");
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      toast.success("File downloaded successfully");
    } else {
      toast.error("Failed to download file");
    }
  };

  useEffect(() => {
    fetchSentSmsData();
  }, []);

  return (
    <>
      <div className="w-full overflow-y-scroll h-full">
        <InsideHeader
          title="Sent SMS"
          subtitle="This is a list of all sms you have sent"
          back={false}
        />

        <div className="lg:px-10 px-3">
          <div className="flex flex-col">
            <div className="mt-[1.31rem] flex justify-between items-center gap-x-10">
              <div className="flex justify-start gap-x-10">
                {user?.layer === "ACCOUNT" && (
                  <div className={`w-[250px]`}>
                    <button
                      className={`cstm-btn  !rounded-[4px] !bg-[#A3A2A7] !text-[.75rem] flex items-center gap-x-3`}
                      onClick={handleSendSms}
                    >
                      <img src={svg32} alt="svg32" />
                      Send New Message
                    </button>
                  </div>
                )}

                <div className="flex items-center">
                  <span>
                    {" "}
                    <button
                      onClick={showModal}
                      type="button"
                      className={`bg-transparent flex items-center gap-x-'1' ${
                        Object?.keys(formData)?.length > 0
                          ? "!text-[#5688E5]"
                          : "inherit"
                      }`}
                    >
                      <MaterialIcon color="#141414" icon="filter_list" />
                      Filters
                    </button>
                  </span>
                  {Object?.keys(formData)?.length > 0 && (
                    <span className="flex items-center text-[#5688E5] cursor-pointer ml-1">
                      :{Object?.keys(formData)?.length}
                      <img
                        src={svg38}
                        alt="svg38"
                        onClick={handleClearFilters}
                      />
                    </span>
                  )}
                </div>
              </div>

              {sentSmsData?.length > 0 && (
                <div className="flex justify-end item-center">
                  {saving ? (
                    <Spin className="sms-spin" />
                  ) : (
                    <Tooltip placement="top" title={"Download Excel"}>
                      <button
                        onClick={handleClick}
                        className="flex items-center"
                      >
                        <MaterialIcon
                          size={45}
                          color="#00B050"
                          icon="article"
                        />
                        <span>Export to excel</span>
                      </button>
                    </Tooltip>
                  )}
                </div>
              )}
            </div>
            <div className="ml-[20%]"></div>
          </div>
          {loadingSms ? (
            <Skeleton />
          ) : (
            <div>
              {sentSmsData && sentSmsData?.length > 0 ? (
                <Table
                  className="mt-[1.31rem] w-full mb-10"
                  scroll={{
                    // x: "max-content",
                    x: 1600,
                  }}
                  pagination={{
                    position: ["bottomCenter"],
                    current: pageIndex + 1,
                    total: sentSmsCount,
                    pageSize: pageSize,
                    onChange: (page, size) => {
                      setPageIndex(page - 1);
                      setPageSize(size);
                      fetchSentSmsData(page - 1, size);
                    },
                    showSizeChanger: false,
                    hideOnSinglePage: true,
                  }}
                  rowKey={(record) => record?.msgId}
                  columns={columns}
                  dataSource={sentSmsData}
                  loading={loadingSms}
                />
              ) : (
                <div className="flex flex-col items-center justify-center mt-10">
                  <img src={noCon} alt="noCon" />
                  <span className="text-black21 text-[18px] font-normal leading-[24px] font-dmSans">
                    No Sms Found
                  </span>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
      <FilterModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        formData={formData}
        setFormData={setFormData}
      />
    </>
  );
}

export default SentSmsList;
