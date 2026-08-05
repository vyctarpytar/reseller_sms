import React, { useEffect, useState } from "react";
import { Drawer, Pagination, Skeleton } from "antd";
import { useDispatch } from "react-redux";
import { fetchNotificationLogs } from "../../features/save/saveSlice";
import StatusBadge from "../../components/StatusBadge";
import { formatDateTime } from "../../utils";

const PAGE_SIZE = 10;

const NotificationLogsDrawer = ({ open, onClose, prodd }) => {
  const dispatch = useDispatch();
  const [logs, setLogs] = useState([]);
  const [total, setTotal] = useState(0);
  const [pageIndex, setPageIndex] = useState(0);
  const [loading, setLoading] = useState(false);

  async function loadLogs(start = 0) {
    if (!prodd?.snId) return;
    setLoading(true);
    const res = await dispatch(
      fetchNotificationLogs({
        url: `api/v2/notifications/logs/${prodd?.snId}`,
        start,
        limit: PAGE_SIZE,
      })
    );
    if (res?.payload?.success) {
      setLogs(res?.payload?.data?.result || []);
      setTotal(res?.payload?.total || 0);
    } else {
      setLogs([]);
      setTotal(0);
    }
    setLoading(false);
  }

  useEffect(() => {
    if (!open) return;
    setPageIndex(0);
    loadLogs(0);
  }, [open, prodd?.snId]);

  return (
    <Drawer
      title={prodd?.snName ? `Run history — ${prodd?.snName}` : "Run history"}
      placement="right"
      width={620}
      open={open}
      onClose={onClose}
    >
      {loading ? (
        <Skeleton active paragraph={{ rows: 6 }} />
      ) : logs?.length > 0 ? (
        <div>
          <div className="space-y-2.5">
            {logs.map((item) => (
              <div key={item?.snlId} className="card !p-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-[13px] font-medium text-primary">
                      {formatDateTime(item?.snlRunAt)}
                    </p>
                    <p className="text-[12px] text-muted mt-1.5">
                      SMS {item?.snlSmsSent ?? 0} sent / {item?.snlSmsFailed ?? 0}{" "}
                      failed · Email {item?.snlEmailSent ?? 0} sent /{" "}
                      {item?.snlEmailFailed ?? 0} failed
                    </p>
                    {item?.snlDetail && (
                      <p className="text-[12px] text-muted mt-1.5 break-words">
                        {item?.snlDetail}
                      </p>
                    )}
                  </div>
                  <div className="text-right shrink-0">
                    <StatusBadge value={item?.snlStatus} />
                    <p className="text-[11px] text-muted mt-1.5 whitespace-nowrap">
                      {item?.snlTriggeredBy || "—"}
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>
          {total > PAGE_SIZE && (
            <div className="flex justify-center mt-4">
              <Pagination
                size="small"
                current={pageIndex + 1}
                total={total}
                pageSize={PAGE_SIZE}
                showSizeChanger={false}
                onChange={(page) => {
                  setPageIndex(page - 1);
                  loadLogs(page - 1);
                }}
              />
            </div>
          )}
        </div>
      ) : (
        <div className="card !p-8 text-center text-muted text-sm">No runs yet</div>
      )}
    </Drawer>
  );
};

export default NotificationLogsDrawer;
