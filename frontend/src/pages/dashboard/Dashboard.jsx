import React, { useEffect, useState } from "react";
import DashboardCard from "./DashboardCard";
import DashTimeseries from "./DashTimeseries";
import TopSummaryCards from "./TopSummaryCards";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { fetchDash } from "../../features/dashboard/dashboardSlice";
import { Skeleton } from "antd";
import MaterialIcon from "material-icons-react";
import FilterModal from "./FilterModal";
import { cleanLegendClickStatus } from "../../features/global/globalSlice";
import { formatDate, getDate30DaysAgo, getDate7DaysAgo } from "../../utils";

function Dashboard() {
  const { dashData, loading } = useSelector((state) => state.dash);
  const { balanceHeader } = useSelector((state) => state.menu);
  const { legendClickStatus } = useSelector((state) => state.global);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [initialLoad, setInitialLoad] = useState(true);
  const [formData, setFormData] = useState({});
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [legendClick, setLegendClick] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };

  const handleClearFilter = async (event) => {
    await setFormData({});
    await setLegendClick(false);
    await setActiveBtn("DAY");
    await dispatch(cleanLegendClickStatus());
    await dispatch(
      fetchDash({
        msgStatus: null,
        url: "api/v2/dash",
      })
    );
  };
  const [activeBtn, setActiveBtn] = useState("DAY");
  const today = new Date();

  const handleFetchDayData = () => {
    if (activeBtn === "WEEK") {
      dispatch(
        fetchDash({
          msgDateFrom: getDate7DaysAgo(),
          msgDateTo: formatDate(today),
          url: "api/v2/dash",
        })
      );
    }
    if (activeBtn === "MONTH") {
      dispatch(
        fetchDash({
          msgDateFrom: getDate30DaysAgo(),
          msgDateTo: formatDate(today),
          url: "api/v2/dash",
        })
      );
    }
  };
  const handleClick = async (item) => {
    await setActiveBtn(item);
    await handleFetchDayData();
  };
  async function fetchDashData() {
    dispatch(
      fetchDash({
        url: "api/v2/dash",
      })
    );
  }

  async function fetchFilteredData() {
    dispatch(
      fetchDash({
        msgStatus: formData?.msgStatus,
        msgCreatedDate: formData?.msgCreatedDate,
        msgSubmobileNo: formData?.msgSubmobileNo,
        msgDateFrom: formData?.msgDateFrom,
        msgDateTo: formData?.msgDateTo,
        msgResellerId: formData?.msgResellerId,
        msgSenderId: formData?.msgSenderId,
        url: "api/v2/dash",
      })
    );
  }

  const handleLegendClick = async () => {
    await dispatch(
      fetchDash({
        msgStatus: legendClickStatus,
        url: "api/v2/dash",
      })
    );
  };

  useEffect(() => {
    if (Object.keys(formData).length > 0) {
      fetchFilteredData();
    } else if (legendClickStatus) {
      handleLegendClick();
    } else if (activeBtn === "WEEK" || activeBtn === "MONTH") {
      handleFetchDayData();
    } else {
      fetchDashData();
    }

    setInitialLoad(false);
  }, [formData, legendClickStatus, activeBtn]);

  // useEffect(() => {
  //   if (Object.keys(formData).length > 0) {
  //     fetchFilteredData();
  //   } else if (legendClickStatus) {
  //     handleLegendClick();
  //   } else if (activeBtn === "WEEK" || activeBtn === "MONTH") {
  //     handleFetchDayData();
  //   }
  //   else {
  //     fetchDashData();
  //   }
  //   setInitialLoad(false);

  //   const intervalId = setInterval(() => {
  //     if (Object.keys(formData).length > 0) {
  //       fetchFilteredData();
  //     }else if (legendClickStatus) {
  //       handleLegendClick();
  //     } else if (activeBtn === "WEEK" || activeBtn === "MONTH") {
  //       handleFetchDayData();
  //     }
  //      else {
  //       fetchDashData();
  //     }
  //   }, 10000);

  //   return () => clearInterval(intervalId);
  // }, [formData,legendClickStatus,activeBtn]);

  return (
    <>
      <div className="w-full h-full overflow-y-scroll bg-surface lg:px-10 lg:py-8 py-5 px-3">
        {initialLoad && loading ? (
          <Skeleton active />
        ) : (
          <>
            <p className="sa-eyebrow">Overview</p>
            <h1 className="sa-title text-[26px]">{balanceHeader?.accName}</h1>
            <p className="text-sm text-muted mt-1 mb-6">
              Total SMS summary in your account
            </p>

            {/* Platform money + derived units overview (TOP only). */}
            <TopSummaryCards />

            <div className="flex lg:flex-row flex-col lg:items-center justify-between gap-4 mb-7">
              {/* segmented range control */}
              <div className="inline-flex items-center rounded-xl border border-border bg-white p-1 shadow-card w-fit">
                {[
                  { key: "DAY", label: "Day" },
                  { key: "WEEK", label: "Week" },
                  { key: "MONTH", label: "Month" },
                ].map((b) => (
                  <button
                    key={b.key}
                    onClick={() => handleClick(b.key)}
                    className={`px-5 py-1.5 rounded-lg text-sm font-medium transition-all ${
                      activeBtn === b.key
                        ? "bg-primary text-white shadow-card"
                        : "text-ink hover:text-primary"
                    }`}
                  >
                    {b.label}
                  </button>
                ))}
              </div>

              <div className="flex items-center gap-3">
                <button
                  onClick={showModal}
                  type="button"
                  className="btn-outline !py-2"
                >
                  <MaterialIcon color="#69472E" icon="filter_list" />
                  Advanced Filters
                  {Object?.keys(formData)?.length > 0 && (
                    <span className="ml-1 h-2 w-2 rounded-full bg-accent" />
                  )}
                </button>

                <button
                  className="btn-ghost text-muted hover:text-primary"
                  onClick={handleClearFilter}
                >
                  <MaterialIcon color="currentColor" icon="close" size={18} />
                  Clear
                </button>
              </div>
            </div>
            {dashData?.msgTimeSeries?.length > 0 &&
            dashData?.ststusSummary?.length > 0 ? (
              <>
                <DashboardCard dashData={dashData} />
                <DashTimeseries initialLoad={initialLoad} />
              </>
            ) : (
              <div className="card flex flex-col items-center justify-center text-center py-20 px-6">
                <div
                  className="h-20 w-20 rounded-full flex items-center justify-center"
                  style={{ background: "rgba(217,108,59,0.10)" }}
                >
                  <MaterialIcon
                    icon="insights"
                    color="var(--brand-accent)"
                    size={40}
                  />
                </div>
                <h3 className="mt-6 text-lg font-medium text-primary leading-snug">
                  No SMS activity yet
                </h3>
                <p className="mt-2 max-w-sm text-sm text-muted leading-relaxed">
                  There's no data for this account in the selected period. Try a
                  wider range or clear your filters.
                </p>
                <button
                  onClick={handleClearFilter}
                  className="btn-outline mt-6 !py-2 !px-5"
                >
                  Clear filters
                </button>
              </div>
            )}
          </>
        )}
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

export default Dashboard;
