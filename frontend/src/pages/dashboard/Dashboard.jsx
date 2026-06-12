import React, { useEffect, useState } from "react";
import DashboardCard from "./DashboardCard";
import DashTimeseries from "./DashTimeseries";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { fetchDash } from "../../features/dashboard/dashboardSlice";
import noDataDash from "../../assets/img/dashNoData.jpg";
import { Skeleton } from "antd";
import MaterialIcon from "material-icons-react";
import FilterModal from "./FilterModal";
import svg41 from "../../assets/svg/svg41.svg";
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
      <div className="w-full h-full overflow-y-scroll bg-lightBlue lg:px-10 lg:py-10 py-5 px-3">
        {initialLoad && loading ? (
          <Skeleton />
        ) : (
          <>
            <div className="grap-title">{balanceHeader?.accName}</div>
            <div className="mt-[.81rem] mb-[1rem] grap-sub-title flex justify-between">
              Total SMS summary in your account
            </div>
            <div className="flex lg:flex-row flex-col mb-5 mt-2">
              <div className="flex items-center gap-x-0">
                <div className="w-[80px]">
                  <button
                    onClick={() => handleClick("DAY")}
                    className={`${
                      activeBtn === "DAY"
                        ? "!bg-darkGreen !text-white"
                        : "!bg-white !text-darkGreen !border !border-darkGreen"
                    } cstm-btn !rounded-[4px]`}
                  >
                    Day
                  </button>
                </div>
                <div className="w-[80px]">
                  <button
                    onClick={() => handleClick("WEEK")}
                    className={`${
                      activeBtn === "WEEK"
                        ? "!bg-darkGreen !text-white"
                        : "!bg-white !text-darkGreen !border !border-darkGreen"
                    } cstm-btn !rounded-[4px]`}
                  >
                    Week
                  </button>
                </div>
                <div className="w-[80px]">
                  <button
                    onClick={() => handleClick("MONTH")}
                    className={`${
                      activeBtn === "MONTH"
                        ? "!bg-darkGreen !text-white"
                        : "!bg-white !text-darkGreen !border !border-darkGreen"
                    } cstm-btn !rounded-[4px]`}
                  >
                    month
                  </button>
                </div>
              </div>

              <div className="flex lg:ml-[20rem] lg:justify-end justify-start items-center lg:mt-0 mt-5">
                <div className="w-[220px]">
                  <button
                    onClick={showModal}
                    type="button"
                    className={`cstm-btn !bg-white !text-darkGreen !border !border-darkGreen !rounded-[4px] ${
                      Object?.keys(formData)?.length > 0
                        ? "!text-[#5688E5]"
                        : "inherit"
                    }`}
                  >
                    <MaterialIcon color="#388E3C" icon="filter_list" />
                    Advanced Filters
                  </button>
                </div>

                <div className="lg:ml-10 ml-2">
                  <span
                    className="cursor-pointer font-lexendS flex items-center text-darkGreen text-[16px] font-[600]"
                    onClick={handleClearFilter}
                  >
                    <img src={svg41} alt="svg41" />
                    Clear Filters
                  </span>
                </div>
              </div>
            </div>
            {dashData?.msgTimeSeries?.length > 0 &&
            dashData?.ststusSummary?.length > 0 ? (
              <>
                <DashboardCard dashData={dashData} />
                <DashTimeseries initialLoad={initialLoad} />
              </>
            ) : (
              <div className="flex flex-col justify-center items-center">
                <img
                  className="h-[60vh] object-cover"
                  src={noDataDash}
                  alt="noDataDash"
                />
                <span className="mt-10 grap-title">
                  No Dashboard Data Available
                </span>
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
