import { Skeleton, Spin, Table, Tooltip } from "antd";
import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { fetchDailySmsReport } from "../../../features/dashboard/dashboardSlice";
import {
  dateForHumans,
  formatDate,
  getDate7DaysAgo,
  normalizeDateToLocalYear,
  numberWithCommas,
} from "../../../utils";
import MaterialIcon from "material-icons-react";
import svg38 from "../../../assets/svg/svg38.svg";
import FilterDailySmsModal from "./FilterDailySmsModal";
import { downloadExcel } from "../../../features/save/saveSlice";
import toast from "react-hot-toast";

function DailySms() {
  const dispatch = useDispatch();
  const { daiySmsCount, dailySmsData, loading } = useSelector(
    (state) => state.dash
  );
  const { user } = useSelector((state) => state.auth);
  const { saving } = useSelector((state) => state.save);

  const [formData, setFormData] = useState({});
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [initialLoad, setInitialLoad] = useState(true);

  const today = new Date();

  async function fetchDailySmsReportData(page, size) {
    dispatch(
      fetchDailySmsReport({
        msgDateFrom:
          normalizeDateToLocalYear(formData?.msgDateFrom) ?? getDate7DaysAgo(),
        msgDateTo:
          normalizeDateToLocalYear(formData?.msgDateTo) ?? formatDate(today),
        msgAccId: user?.layer != "ACCOUNT" ? formData?.msgAccId : null,
        url: "api/v2/rpt/daily-sms-usage",
        limit: size ?? pageSize,
        start: page ?? pageIndex,
      })
    );
  }

  const columns = [
    {
      title: "Created Date",
      render: (item) => {
        return <div>{dateForHumans(item)}</div>;
      },
      dataIndex: "createdDate",
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
        url: "api/v2/rpt/daily-sms-usage-download-excel",
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
      link.setAttribute("download", "daily-sms-usage.xlsx");
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      toast.success("File downloaded successfully");
    } else {
      toast.error("Failed to download file");
    }
  };

  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };

  const handleClearFilters = async () => {
    await setFormData({});
    const res = await dispatch(
      fetchDailySmsReport({
        msgDateFrom: getDate7DaysAgo(),
        msgDateTo: formatDate(today),
        msgAccId: null,
        url: "api/v2/rpt/daily-sms-usage",
      })
    );
  };

  useEffect(() => {
    fetchDailySmsReportData();
    setInitialLoad(false);
  }, []);

  // useEffect(() => {
  //   fetchDailySmsReportData();
  //   setInitialLoad(false);

  //   const intervalId = setInterval(() => {
  //     if (Object.keys(formData).length > 0) {
  //       fetchDailySmsReportData();
  //     } else {
  //       fetchDailySmsReportData();
  //     }
  //   }, 20000);

  //   return () => clearInterval(intervalId);
  // }, [formData]);
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
          dataSource={dailySmsData}
          loading={loading}
          pagination={{
            position: ["bottomCenter"],
            current: pageIndex + 1,
            total: daiySmsCount,
            pageSize: pageSize,
            onChange: (page, size) => {
              setPageIndex(page - 1);
              setPageSize(size);
              fetchDailySmsReportData(page - 1, size);
            },
            showSizeChanger: false,
            hideOnSinglePage: true,
          }}
        />
      )}

      <FilterDailySmsModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        formData={formData}
        setFormData={setFormData}
      />
    </div>
  );
}

export default DailySms;
