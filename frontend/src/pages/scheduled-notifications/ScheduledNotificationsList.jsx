import React, { useEffect, useRef, useState } from "react";
import { Dropdown, Input, Select } from "antd";
import { Link, useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import toast from "react-hot-toast";
import InsideHeader from "../../components/InsideHeader";
import StatusBadge from "../../components/StatusBadge";
import ConfirmModal from "../../components/ConfirmModal";
import ResponsiveTable, { hideBelow } from "../../components/ResponsiveTable";
import ScheduledNotificationModal, {
  frequencyLabel,
  splitCsv,
} from "./ScheduledNotificationModal";
import NotificationLogsDrawer from "./NotificationLogsDrawer";
import {
  deleteRequest,
  fetchNotifications,
  save,
} from "../../features/save/saveSlice";
import { formatDateTime } from "../../utils";
import useModalToggle from "../../custom_hooks/useModalToggle";
import noCon from "../../assets/img/noCon.png";
import svg2 from "../../assets/svg/svg2.svg";
import svg26 from "../../assets/svg/svg26.svg";
import svg27 from "../../assets/svg/svg27.svg";
import svg32 from "../../assets/svg/svg32.svg";
import svg34 from "../../assets/svg/svg34.svg";
import svg36 from "../../assets/svg/svg36.svg";
import svg45 from "../../assets/svg/svg45.svg";

const truncateText = (text, maxLength) => {
  if (text?.length > maxLength) return text?.substring(0, maxLength - 3) + "...";
  return text;
};

const sendTimesLabel = (item) =>
  splitCsv(item?.snSendTimes || item?.snSendTime).join(", ") || "—";

function ScheduledNotificationsList() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { user } = useSelector((state) => state.auth);
  const { notificationsData, notificationsCount, loadingNotifications, saving } =
    useSelector((state) => state.save);

  const [prodd, setProdd] = useState();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [openDelete, setOpenDelete] = useState(false);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState(null);
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const { open, handleOpen, handleCancel } = useModalToggle();

  useEffect(() => {
    if (user?.layer !== "TOP") navigate("/roles-error-page");
  }, [user]);

  async function loadNotifications(overrides = {}) {
    await dispatch(
      fetchNotifications({
        url: "api/v2/notifications/list",
        start: overrides.start ?? pageIndex,
        limit: overrides.limit ?? pageSize,
        snStatus: overrides.snStatus !== undefined ? overrides.snStatus : statusFilter,
        search: overrides.search !== undefined ? overrides.search : search,
      })
    );
  }

  const firstLoad = useRef(true);
  useEffect(() => {
    if (firstLoad.current) {
      firstLoad.current = false;
      loadNotifications({ start: 0, search: "", snStatus: null });
      return;
    }
    const timer = setTimeout(() => {
      setPageIndex(0);
      loadNotifications({ start: 0, search, snStatus: statusFilter });
    }, 400);
    return () => clearTimeout(timer);
  }, [search, statusFilter]);

  const handleAdd = () => {
    setProdd(null);
    setIsModalOpen(true);
  };

  const handleEdit = () => setIsModalOpen(true);

  const handleRunNow = async () => {
    const res = await dispatch(
      save({ url: `api/v2/notifications/run/${prodd?.snId}` })
    );
    if (res?.payload?.success) {
      toast.success(res?.payload?.messages?.message);
      loadNotifications();
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  const handleToggle = async () => {
    const res = await dispatch(
      save({ url: `api/v2/notifications/toggle/${prodd?.snId}` })
    );
    if (res?.payload?.success) {
      toast.success(res?.payload?.messages?.message);
      loadNotifications();
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  const handleDelete = async () => {
    const res = await dispatch(
      deleteRequest({ url: `api/v2/notifications/delete/${prodd?.snId}` })
    );
    if (res?.payload?.success) {
      toast.success(res?.payload?.messages?.message);
      setOpenDelete(false);
      loadNotifications();
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  const settingItems = [
    {
      key: "edit",
      label: (
        <Link
          className="flex gap-x-[.75rem] items-center py-[.5rem]"
          onClick={handleEdit}
        >
          <img src={svg26} alt="edit" className="w-4 h-4" /> Edit
        </Link>
      ),
    },
    {
      key: "run",
      label: (
        <Link
          className="flex gap-x-[.75rem] items-center py-[.5rem]"
          onClick={handleRunNow}
        >
          <img src={svg32} alt="run" className="w-4 h-4" /> Run now
        </Link>
      ),
    },
    {
      key: "toggle",
      label: (
        <Link
          className="flex gap-x-[.75rem] items-center py-[.5rem]"
          onClick={handleToggle}
        >
          <img
            src={prodd?.snStatus === "ACTIVE" ? svg36 : svg34}
            alt="toggle"
            className="w-4 h-4"
          />
          <span className="whitespace-nowrap">
            {prodd?.snStatus === "ACTIVE" ? "Pause" : "Activate"}
          </span>
        </Link>
      ),
    },
    {
      key: "logs",
      label: (
        <Link
          className="flex gap-x-[.75rem] items-center py-[.5rem]"
          onClick={handleOpen}
        >
          <img src={svg2} alt="history" className="w-4 h-4" />
          <span className="whitespace-nowrap">View run history</span>
        </Link>
      ),
    },
    { key: "divider-1", type: "divider" },
    {
      key: "delete",
      label: (
        <Link
          className="flex gap-x-[.75rem] items-center py-[.5rem]"
          onClick={() => setOpenDelete(true)}
        >
          <img src={svg45} alt="delete" className="w-4 h-4" /> Delete
        </Link>
      ),
    },
  ];

  const channelPills = (channels) => (
    <div className="flex items-center gap-x-1.5">
      {splitCsv(channels).map((channel) => (
        <span key={channel} className="badge-brand !text-[10px]">
          {channel}
        </span>
      ))}
    </div>
  );

  const columns = [
    {
      title: "Reminder",
      render: (item) => (
        <div className="min-w-0">
          <p className="font-medium text-primary">{item?.snName || "—"}</p>
          <p className="text-[11px] text-muted mt-1">
            {truncateText(item?.snMessage, 60) || "—"}
          </p>
        </div>
      ),
    },
    {
      title: "Schedule",
      render: (item) => (
        <div>
          <p>{frequencyLabel(item?.snFrequency)}</p>
          <p className="text-[11px] text-muted mt-1">at {sendTimesLabel(item)}</p>
        </div>
      ),
    },
    {
      title: "Channels",
      render: (item) => channelPills(item?.snChannels),
    },
    {
      title: "Recipients",
      ...hideBelow(),
      render: (item) => (
        <span className="whitespace-nowrap">
          {splitCsv(item?.snRecipients).length} phone ·{" "}
          {splitCsv(item?.snEmails).length} email
        </span>
      ),
    },
    {
      title: "Next run",
      render: (item) => <span>{formatDateTime(item?.snNextRunAt)}</span>,
    },
    {
      title: "Last run",
      ...hideBelow(),
      render: (item) =>
        item?.snLastRunAt ? (
          <div>
            <p>{formatDateTime(item?.snLastRunAt)}</p>
            <div className="mt-1">
              <StatusBadge value={item?.snLastStatus} />
            </div>
          </div>
        ) : (
          <span className="text-muted">—</span>
        ),
    },
    {
      title: "Status",
      render: (item) => (
        <div className="flex items-center justify-center">
          <StatusBadge value={item?.snStatus} />
        </div>
      ),
    },
    {
      title: "Actions",
      render: (item) => (
        <Dropdown
          overlayStyle={{ width: "250px" }}
          trigger={["click"]}
          menu={{ items: settingItems }}
          placement="bottom"
        >
          <button onClick={() => setProdd(item)}>
            <img src={svg27} alt="actions" />
          </button>
        </Dropdown>
      ),
    },
  ];

  const hasRows = notificationsData && notificationsData?.length > 0;

  return (
    <>
      <div className="w-full h-full overflow-y-scroll bg-surface">
        <InsideHeader
          title="Reminders"
          subtitle="Recurring SMS & email notifications"
          back={false}
        />

        <div className="lg:px-10 px-3">
          <div className="mt-[1.31rem] flex flex-wrap justify-between items-center gap-4">
            <div className="w-[200px]">
              <button
                className="cstm-btn !rounded-[4px] !text-[.75rem]"
                onClick={handleAdd}
              >
                Add Reminder
              </button>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <Select
                allowClear
                placeholder="All statuses"
                className="w-[160px]"
                style={{ height: "42px" }}
                value={statusFilter}
                onChange={(value) => setStatusFilter(value ?? null)}
                options={[
                  { value: "ACTIVE", label: "Active" },
                  { value: "PAUSED", label: "Paused" },
                ]}
              />
              <Input
                allowClear
                className="input !w-[240px]"
                placeholder="Search reminders"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          </div>

          <div className="mt-[1.31rem] mb-10 card !p-0 overflow-hidden">
            {hasRows || loadingNotifications ? (
              <ResponsiveTable
                className="w-full"
                scroll={{ x: "auto" }}
                pagination={{
                  position: ["bottomCenter"],
                  current: pageIndex + 1,
                  total: notificationsCount,
                  pageSize: pageSize,
                  onChange: (page, size) => {
                    setPageIndex(page - 1);
                    setPageSize(size);
                    loadNotifications({ start: page - 1, limit: size });
                  },
                  showSizeChanger: false,
                  hideOnSinglePage: true,
                }}
                rowKey={(r) => r?.snId}
                columns={columns}
                dataSource={notificationsData}
                loading={loadingNotifications}
                mobileEmptyText="No reminders found"
                mobileCard={(record) => (
                  <div className="card !p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <p className="font-medium text-primary truncate">
                          {record?.snName || "—"}
                        </p>
                        <p className="text-[11px] text-muted mt-1.5 truncate">
                          {truncateText(record?.snMessage, 60) || "—"}
                        </p>
                        <p className="text-[11px] text-muted mt-1.5">
                          {frequencyLabel(record?.snFrequency)} at{" "}
                          {sendTimesLabel(record)}
                        </p>
                        <div className="mt-2">{channelPills(record?.snChannels)}</div>
                      </div>
                      <div className="text-right shrink-0">
                        <StatusBadge value={record?.snStatus} />
                        <p className="text-[11px] text-muted whitespace-nowrap mt-1.5">
                          {formatDateTime(record?.snNextRunAt)}
                        </p>
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
                            <img src={svg27} alt="actions" />
                          </button>
                        </Dropdown>
                      </div>
                    </div>
                  </div>
                )}
              />
            ) : (
              <div className="card flex flex-col items-center justify-center text-center py-16 px-6 max-w-md mx-auto my-10">
                <img
                  src={noCon}
                  alt=""
                  className="h-28 w-28 object-contain opacity-90"
                />
                <h3 className="mt-5 text-lg font-medium text-primary">
                  Set up your first reminder
                </h3>
                <p className="mt-2 text-sm text-muted">
                  Send recurring SMS and email notifications automatically
                </p>
                <div className="w-[200px] mt-6">
                  <button className="cstm-btn" onClick={handleAdd}>
                    Add Reminder
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      <ScheduledNotificationModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        prodd={prodd}
        handleFetch={loadNotifications}
      />

      <NotificationLogsDrawer open={open} onClose={handleCancel} prodd={prodd} />

      <ConfirmModal
        open={openDelete}
        handleCancel={() => setOpenDelete(false)}
        handleSubmit={handleDelete}
        loading={saving}
        content={`Are you sure you want to delete "${prodd?.snName || ""}"?`}
        type="alert"
        btnTitle="Confirm"
      />
    </>
  );
}

export default ScheduledNotificationsList;
