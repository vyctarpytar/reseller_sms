import { Badge, Dropdown, Skeleton, Spin, Tooltip } from "antd";
import React, { useEffect, useRef, useState } from "react";
import ResponsiveTable, { hideBelow } from "../../components/ResponsiveTable";
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
  getStartOfWeek,
} from "../../utils";
import noCon from "../../assets/img/noCon.png";
import svg38 from "../../assets/svg/svg38.svg";
import FilterModal from "./FilterModal";
import {
  downloadExcel,
  fetchSavedSms,
  fetchSentSmsSummary,
  save,
} from "../../features/save/saveSlice";
import SentSmsSummary from "./SentSmsSummary";
import StatusBadge from "../../components/StatusBadge";
import ExportExcelButton from "../../components/ExportExcelButton";
import toast from "react-hot-toast";

function SentSmsList() {
  const [notOpen, setnotOpen] = useState(false);
  const { loading } = useSelector((state) => state.sms);
  const { user } = useSelector((state) => state.auth);
  const { saving } = useSelector((state) => state.save);
  const { sentSmsData, loadingSms, sentSmsCount, sentSmsSummary, loadingSummary } =
    useSelector((state) => state.save);

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
      ...hideBelow(),
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
            ...hideBelow(),
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
      ...hideBelow(),
      dataIndex: "msgAccName",
      width: "10%",
    },
    {
      title: "Sender Name",
      ...hideBelow(),
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
      render: (item) => (
        <div className="flex items-center justify-center">
          <StatusBadge value={item?.msgStatus} />
        </div>
      ),
      width: "10%",
    },
    {
      title: "Pages",
      ...hideBelow(),
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
      render: (item) => <div className="whitespace-nowrap">{formatDateTime(item)}</div>,
      dataIndex: "msgCreatedDate",
      width: "10%",
    },
    {
      title: "Sent By",
      ...hideBelow(),
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
    setActiveCard(null);
    setCardStatuses(null);
    setPageIndex(0);
    const res = await dispatch(
      fetchSavedSms({
        url: "api/v2/sms",
        msgStatus: null,
        msgStatusList: null,
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

  // Summary-card quick filter: which card is active and the raw statuses it maps
  // to. Kept separate from `formData` (the modal filters) so toggling a card
  // doesn't reload the summary strip or touch the filter-count badge.
  const [activeCard, setActiveCard] = useState(null);
  const [cardStatuses, setCardStatuses] = useState(null);

  async function fetchSentSmsData(page, size, statuses = cardStatuses) {
    const res = await dispatch(
      fetchSavedSms({
        url: "api/v2/sms",
        limit: size ?? pageSize,
        start: page ?? pageIndex,
        msgStatus: formData?.msgStatus,
        msgStatusList: statuses?.length ? statuses : null,
        msgCreatedFrom: formData?.msgCreatedFrom,
        msgCreatedTo: formData?.msgCreatedTo,
        msgSubmobileNo: formData?.msgSubmobileNo,
        msgMessage: formData?.msgMessage,
        msgAccId: formData?.msgAccId,
        msgSenderId: formData?.msgSenderId,
      })
    );
  }

  // Clicking a summary card filters the table to that bucket's statuses.
  // Clicking the active card again (or "Total") clears the bucket filter.
  const handleCardFilter = (key, statuses) => {
    const turningOff = activeCard === key || key === "total" || !statuses?.length;
    const nextStatuses = turningOff ? null : statuses;
    setActiveCard(turningOff ? null : key);
    setCardStatuses(nextStatuses);
    setPageIndex(0);
    fetchSentSmsData(0, pageSize, nextStatuses);
  };

  // Server-aggregated per-status summary. Uses the status report endpoint (works
  // across a reseller's accounts, unlike api/v2/dash which is account-scoped).
  // Defaults to the current week (Monday → today) — all-time aggregation is
  // expensive. The user widens/narrows the window via the date filter.
  function fetchSummary() {
    const todayStr = formatDate(new Date());
    const weekStartStr = getStartOfWeek();
    dispatch(
      fetchSentSmsSummary({
        url: "api/v2/rpt/status-sms-usage",
        msgDateFrom: formData?.msgCreatedFrom ?? weekStartStr,
        msgDateTo: formData?.msgCreatedTo ?? todayStr,
        msgAccId: formData?.msgAccId ?? null,
        limit: 1000,
        start: 0,
      })
    );
  }

  const handleClick = async (item) => {
    const res = await dispatch(
      downloadExcel({
        url: "api/v2/sms/download-excel",
        msgStatus: formData?.msgStatus,
        msgStatusList: cardStatuses?.length ? cardStatuses : null,
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

  // Summary is page-independent — refresh it on mount and whenever filters change.
  useEffect(() => {
    fetchSummary();
  }, [formData]);

  return (
    <>
      <div className="w-full overflow-y-scroll h-full bg-surface">
        <InsideHeader
          title="Sent SMS"
          subtitle="This is a list of all sms you have sent"
          back={false}
        />

        <div className="lg:px-10 px-3">
          <SentSmsSummary
            summary={sentSmsSummary}
            loading={loadingSummary}
            activeKey={activeCard}
            onCardClick={handleCardFilter}
            period={
              formData?.msgCreatedFrom || formData?.msgCreatedTo
                ? `${formData?.msgCreatedFrom ?? "…"} → ${
                    formData?.msgCreatedTo ?? "…"
                  }`
                : "This week"
            }
          />
          <div className="flex flex-col">
            <div className="mt-[1.31rem] flex justify-between items-center gap-x-10">
              <div className="flex justify-start gap-x-10">
                {user?.layer === "ACCOUNT" && (
                  <div className={`w-[250px]`}>
                    <button
                      className={`cstm-btn  !rounded-[4px] !bg-[#69472E] !text-[.75rem] flex items-center gap-x-3`}
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
                      className={`flex items-center gap-x-2 rounded-lg border px-4 py-2 text-[14px] font-medium transition-colors ${
                        Object?.keys(formData)?.length > 0
                          ? "border-[#69472E] text-[#5A4632] bg-[#F7F5F2]"
                          : "border-[#E5E0DA] text-[#5A4632] bg-white hover:bg-[#F7F5F2]"
                      }`}
                    >
                      <MaterialIcon size={20} color="#69472E" icon="filter_list" />
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
                  <ExportExcelButton onClick={handleClick} loading={saving} />
                </div>
              )}
            </div>
            <div className="ml-[20%]"></div>
          </div>
          {loadingSms ? (
            <Skeleton />
          ) : (
            <div className="mt-5 mb-10 card !p-0 overflow-hidden">
              {sentSmsData && sentSmsData?.length > 0 ? (
                <ResponsiveTable
                  className="w-full"
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
                <div className="card flex flex-col items-center justify-center text-center py-16 px-6 max-w-md mx-auto my-10">
                  <img src={noCon} alt="" className="h-28 w-28 object-contain opacity-90" />
                  <h3 className="mt-5 text-lg font-medium text-primary">
                    No SMS found
                  </h3>
                  <p className="mt-2 text-sm text-muted">
                    Sent messages will appear here.
                  </p>
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
