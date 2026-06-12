import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { fetchStatusReport } from "../../../features/dashboard/dashboardSlice";
import {
  dateForHumans,
  formatDate,
  getDate7DaysAgo,
  normalizeDateToLocalYear,
  numberWithCommas,
} from "../../../utils";
import MaterialIcon from "material-icons-react";
import { Skeleton, Spin, Table, Tooltip } from "antd";
import FilterStatusModal from "./FilterStatusModal";
import svg38 from "../../../assets/svg/svg38.svg";
import { downloadExcel } from "../../../features/save/saveSlice";
import toast from "react-hot-toast";

function StatusSummary() {
  const dispatch = useDispatch();
  const { statusSmsCount, statusSmsData, loading } = useSelector(
    (state) => state.dash
  );
  const { user } = useSelector((state) => state.auth);
  const { saving } = useSelector((state) => state.save);
  const [formData, setFormData] = useState({});
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [initialLoad, setInitialLoad] = useState(true);

  const today = new Date();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };

  async function fetchStatusData(page, size) {
    dispatch(
      fetchStatusReport({
        msgDateFrom:
          normalizeDateToLocalYear(formData?.msgDateFrom) ?? getDate7DaysAgo(),
        msgDateTo:
          normalizeDateToLocalYear(formData?.msgDateTo) ?? formatDate(today),
        msgAccId: user?.layer != "ACCOUNT" ? formData?.msgAccId : null,
        url: "api/v2/rpt/status-sms-usage",
        limit: size ?? pageSize,
        start: page ?? pageIndex,
      })
    );
  }

  const columns = [
    {
      title: "Status",
      dataIndex: "msgStatus",
    },
    {
      title: "Messages",
      render: (item) => {
        return <div>{numberWithCommas(item)}</div>;
      },
      dataIndex: "noOfMessages",
    },

    {
      title: "Credit",
      dataIndex: "credit",
    },
  ];
  const handleClick = async (item) => {
    const res = await dispatch(
      downloadExcel({
        url: "api/v2/rpt/status-sms-summary-download-excel",
        msgDateFrom:
          normalizeDateToLocalYear(formData?.msgDateFrom) ?? getDate7DaysAgo(),
        msgDateTo:
          normalizeDateToLocalYear(formData?.msgDateTo) ?? formatDate(today),
        msgAccId: user?.layer != "ACCOUNT" ? formData?.msgAccId : null,
      })
    );
    if (res?.payload) {
      const blob = new Blob([res.payload], {
        type: "application/octet-stream",
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", "status-sms-summary.xlsx");
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      toast.success("File downloaded successfully");
    } else {
      toast.error("Failed to download file");
    }
  };

  const handleClearFilters = async () => {
    await setFormData({});
    const res = await dispatch(
      fetchStatusReport({
        msgDateFrom: getDate7DaysAgo(),
        msgDateTo: formatDate(today),
        msgAccId: null,
        url: "api/v2/rpt/status-sms-usage",
      })
    );
  };

  useEffect(() => {
    fetchStatusData();
    setInitialLoad(false);
  }, []);

  return (
    <div className="w-full h-full overflow-y-scroll lg:px-10 px-3">
      <div className="flex items-center mt-10 justify-between">
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
              <img src={svg38} alt="svg38" onClick={handleClearFilters} />
            </span>
          )}
        </div>
        <div className="flex justify-end item-center">
          <Tooltip placement="top" title={"Download Excel"}>
            {saving ? (
              <Spin className="sms-spin" />
            ) : (
              <button
                disabled={saving}
                onClick={handleClick}
                className="flex items-center"
              >
                <MaterialIcon size={45} color="#00B050" icon="article" />
                <span>Export to excel</span>
              </button>
            )}
          </Tooltip>
        </div>
      </div>
      {loading ? (
        <Skeleton />
      ) : (
        <Table
          className="mt-[1.31rem] w-full"
          scroll={{
            x: 800,
          }}
          rowKey={(record) => record?.reId}
          columns={columns}
          dataSource={statusSmsData}
          loading={loading}
          pagination={{
            position: ["bottomCenter"],
            current: pageIndex + 1,
            total: statusSmsCount,
            pageSize: pageSize,
            onChange: (page, size) => {
              setPageIndex(page - 1);
              setPageSize(size);
              fetchStatusData(page - 1, size);
            },
            showSizeChanger: false,
            hideOnSinglePage: true,
          }}
        />
      )}

      <FilterStatusModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        formData={formData}
        setFormData={setFormData}
      />
    </div>
  );
}

export default StatusSummary;
