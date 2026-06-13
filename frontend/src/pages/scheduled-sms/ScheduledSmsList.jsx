import { Badge, Dropdown, Skeleton, Tooltip } from "antd";
import React, { useEffect, useRef, useState } from "react";
import InsideHeader from "../../components/InsideHeader";
import { Link, useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import svg32 from "../../assets/svg/svg32.svg";
import svg2 from "../../assets/svg/svg2.svg";
import svg27 from "../../assets/svg/svg27.svg";
import svg48 from "../../assets/svg/svg48.svg";
import MaterialIcon from "material-icons-react"; 
import { formatDateTime } from "../../utils";
import noCon from "../../assets/img/noCon.png";
import svg38 from "../../assets/svg/svg38.svg";
import FilterModal from "./FilterModal";
import {
  deleteRequest, 
  fetchScheduledSms, 
} from "../../features/save/saveSlice";
import toast from "react-hot-toast";
import useModalToggle from "../../custom_hooks/useModalToggle";
import ConfirmModal from "../../components/ConfirmModal";
import StatusBadge from "../../components/StatusBadge";
import RescheduleModal from "./RescheduleModal";
import ResponsiveTable, { hideBelow } from "../../components/ResponsiveTable";

function ScheduledSmsList() {
  const [notOpen, setnotOpen] = useState(false);
  const { loading } = useSelector((state) => state.sms);
  const { user } = useSelector((state) => state.auth);
  const { scheduledSmsData, loadingSms, scheduledSmsCount, saving } =
    useSelector((state) => state.save);

  const [formData, setFormData] = useState({});
 

  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };

  const { open, handleOpen, handleCancel } = useModalToggle();
  const [prodd, setProdd] = useState();

  const [openDelete, setOpenDelete] = useState(false);
  const handleOpenDelete = async (item) => {
    await setOpenDelete(true);
  };
  const handleCloseDelete = async () => {
    await setOpenDelete(false);
  };
  const handleDelete = async () => {
    const res = await dispatch(
      deleteRequest({
        url: `api/v2/schedule/delete/${prodd?.schId}`, 
      })
    );
    if (res?.payload?.success) {
      toast.success(res?.payload?.messages?.message);
      setOpenDelete(false);
      fetchscheduledSmsData();
    } else {
      toast.error(res?.payload?.message);
    }
  };

  const handleReschedule = () => {
    handleOpen();
  };

  const truncateText = (text, maxLength) => {
    if (text?.length > maxLength) {
      return text?.substring(0, maxLength - 3) + "...";
    }
    return text;
  };

  const settingItems = [
    {
      key: "1",
      label: (
        <Link
          className="flex gap-x-[.75rem] items-center py-[.5rem]"
          onClick={handleReschedule}
        >
          <img src={svg2} alt="svg2" className="w-4 h-4" /> Reschedule
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
          onClick={() => handleOpenDelete("ARCHIEVE")}
        >
          <img src={svg48} alt="svg48" className="w-4 h-4" />
          <span className="whitespace-nowrap">Delete</span>
        </Link>
      ),
    },
  ];
  const columns = [ 
    {
      title: "Created By Name",
      dataIndex: "schCreatedByName",
      ...hideBelow(),
    },
    {
      title: "Created Date",
      ...hideBelow(),
      render: (item) => {
        return <div>{formatDateTime(item)}</div>;
      },
      dataIndex: "schCreatedOn",
    },
    {
      title: "Message",
      dataIndex: "schMessage",
    },
    {
      title: "Release Time",
      dataIndex: "schReleaseTime",
    },
    {
      title: "Sender ID",
      dataIndex: "schSenderid",
      ...hideBelow(),
    },
    {
      title: "Group Name",
      dataIndex: "schGroupName",
      ...hideBelow(),
    },

    {
      title: "Status",
      render: (item) => (
        <div className="flex items-center justify-center">
          <StatusBadge value={item?.schStatus} />
        </div>
      ),
    },
    {
  title: "Actions",
  render: (item) => (
    item.schStatus === "PENDING" ? (   
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
    ) : null
  ),
}

  ];

  const navigate = useNavigate();
  const dispatch = useDispatch();

  const handleSendSms = () => {
    navigate("/outbox");
  };

  const handleClearFilters = async () => {
    await setFormData({});
    const res = await dispatch(
      fetchScheduledSms({
        url: "api/v2/schedule",
        schUsrId: null,
        schAccId: null,
        schGrpId: null,
      })
    );
  };

  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  async function fetchscheduledSmsData(page, size) {
    const res = await dispatch(
      fetchScheduledSms({
        url: "api/v2/schedule",
        limit: size ?? pageSize,
        start: page ?? pageIndex,
        schUsrId: formData?.schUsrId,
        schAccId: formData?.schAccId,
        schGrpId: formData?.schGrpId,
      })
    );
  }

  useEffect(() => {
    fetchscheduledSmsData();
  }, []);

  return (
    <>
      <div className="w-full overflow-y-scroll h-full bg-surface">
        <InsideHeader
          title="Scheduled SMS"
          subtitle="This is a list of all sms you have scheduled"
          back={false}
        />

        <div className="lg:px-10 px-3">
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
            </div>
            <div className="ml-[20%]"></div>
          </div>
          {loadingSms ? (
            <Skeleton />
          ) : (
            <div className="mt-[1.31rem] mb-10 card !p-0 overflow-hidden">
              {scheduledSmsData && scheduledSmsData?.length > 0 ? (
                <ResponsiveTable
                  className="w-full"
                  scroll={{
                    // x: "max-content",
                    x: "auto",
                  }}
                  pagination={{
                    position: ["bottomCenter"],
                    current: pageIndex + 1,
                    total: scheduledSmsCount,
                    pageSize: pageSize,
                    onChange: (page, size) => {
                      setPageIndex(page - 1);
                      setPageSize(size);
                      fetchscheduledSmsData(page - 1, size);
                    },
                    showSizeChanger: false,
                    hideOnSinglePage: true,
                  }}
                  rowKey={(record) => record?.schId}
                  columns={columns}
                  dataSource={scheduledSmsData}
                  loading={loadingSms}
                  mobileEmptyText="No scheduled SMS found"
                  mobileCard={(record) => (
                    <div className="card !p-4">
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <p className="font-medium text-primary truncate">
                            {truncateText(record?.schMessage, 60) || "—"}
                          </p>
                          <p className="text-[11px] text-muted mt-1.5 truncate">
                            {record?.schSenderid || "—"}
                            {record?.schGroupName ? ` · ${record.schGroupName}` : ""}
                          </p>
                        </div>
                        <div className="text-right shrink-0">
                          <StatusBadge value={record?.schStatus} />
                          <p className="text-[11px] text-muted whitespace-nowrap mt-1.5">
                            {record?.schReleaseTime || "—"}
                          </p>
                          {record?.schStatus === "PENDING" && (
                            <Dropdown
                              overlayStyle={{ width: "250px" }}
                              trigger={["click"]}
                              menu={{ items: settingItems }}
                              placement="bottomRight"
                            >
                              <button
                                className="mt-1.5"
                                onClick={() => setProdd(record)}
                              >
                                <img src={svg27} alt="svg27" />
                              </button>
                            </Dropdown>
                          )}
                        </div>
                      </div>
                    </div>
                  )}
                />
              ) : (
                <div className="card flex flex-col items-center justify-center text-center py-16 px-6 max-w-md mx-auto my-10">
                  <img src={noCon} alt="" className="h-28 w-28 object-contain opacity-90" />
                  <h3 className="mt-5 text-lg font-medium text-primary">
                    No scheduled SMS yet
                  </h3>
                  <p className="mt-2 text-sm text-muted">
                    Messages you schedule will appear here.
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

      <ConfirmModal
        open={openDelete}
        handleCancel={handleCloseDelete}
        handleSubmit={handleDelete}
        loading={saving}
        content={"Are you sure you want to stop this sms"}
        type="alert"
        btnTitle="Confirm"
      />
      <RescheduleModal open={open} handleCancel={handleCancel} prodd={prodd} handleFetch={fetchscheduledSmsData} />
    </>
  );
}

export default ScheduledSmsList;
