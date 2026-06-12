import React, { useEffect, useState } from "react";
import StackedGraph from "./StackedGraph";
import PerformanceGraph from "./PerformanceGraph";
import AnalyticsGraph from "./AnalyticsGraph";
import { fetchReport } from "../../features/dashboard/dashboardSlice";
import { useDispatch } from "react-redux";

function Report() {
  const dispatch =  useDispatch()
  const [formDataR, setFormDataR] = useState({});
  async function fetchAnalyticsData(){  
    dispatch( 
      fetchReport({
        msgStatus: formDataR?.msgStatus,
        msgCreatedDate: formDataR?.msgCreatedDate,
        msgSubmobileNo: formDataR?.msgSubmobileNo,
        msgDateFrom: formDataR?.msgDateFrom,
        msgDateTo: formDataR?.msgDateTo,
        url: "api/v2/rpt",
      })
    );
  }

  useEffect(()=>{
    fetchAnalyticsData()
  },[])
  return (
    <div className="w-full h-full overflow-y-scroll bg-lightBlue lg:px-10 px-3">
      <div className="mt-10 flex lg:flex-row flex-col lg:gap-x-10 gap-y-10  items-center">
        <PerformanceGraph />
        <AnalyticsGraph />
      </div>
      <div className="mt-10 mb-10">
        <StackedGraph />
      </div>
    </div>
  );
}

export default Report;
