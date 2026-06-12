import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { fetchDash } from "../../features/dashboard/dashboardSlice";
import { Skeleton } from "antd";
import FilterModal from "./FilterModal";
import { setLegendClickStatus } from "../../features/global/globalSlice";


const DashTimeseries = ({ initialLoad }) => {
  const dispatch = useDispatch();
  const { balanceHeader } = useSelector((state) => state.menu);
  const { dashData, loading } = useSelector((state) => state.dash);


  const combinedData = dashData?.msgTimeSeries?.map((item) => {
    const transformedItem = {
      msgCreatedDate: item?.msgCreatedDate,
      sentMsgCount: item?.msgStatus === "SENT" ? item?.msgCount : 0,
      DeliveredToTerminal:
        item?.msgStatus === "DeliveredToTerminal" ? item?.msgCount : 0,
      exception: item?.msgStatus === "Exception sending " ? item?.msgCount : 0,
      invalid: item?.msgStatus === "InvalidMsisdn" ? item?.msgCount : 0,
      impossible: item?.msgStatus === "DeliveryImpossible" ? item?.msgCount : 0,
      absent: item?.msgStatus === "AbsentSubscriber" ? item?.msgCount : 0,
      retried: item?.msgStatus === "SENT_RETRIED" ? item?.msgCount : 0,
    };
    return transformedItem;
  });

  const handleLegendClick = async (event) => { 
    await dispatch(setLegendClickStatus(event?.value))
    // await dispatch(
    //   fetchDash({
    //     msgStatus: event?.value,
    //     url: "api/v2/dash",
    //   })
    // );
  };


  return (
    <>
      <div className="graph-card  lg:p-10 p-3 mt-2">
        <div className="grap-title mb-10 mt-5">Sms Status Graph</div>
       

        <div style={{ width: "100%", height: 500 }}>
          <ResponsiveContainer>
            {initialLoad && loading ? (
              <Skeleton />
            ) : (
              <AreaChart
                data={combinedData}
                margin={{ top: 10, right: 30, left: 15, bottom: 40 }}
              >
                <CartesianGrid strokeDasharray="3 3" />

                <XAxis
                  dataKey="msgCreatedDate"
                  label={{
                    value: "Time",
                    position: "insideBottom",
                    offset: -40,
                  }}
                />
                <YAxis
                  label={{
                    value: "Count",
                    angle: -90,
                    position: "insideLeft",
                    offset: -10,
                  }}
                />
                <Tooltip />
                <Area
                  type="monotone"
                  dataKey="sentMsgCount"
                  stroke="#8884d8"
                  fill="#8884d8"
                  name="SENT"
                />
                <Area
                  type="monotone"
                  dataKey="DeliveredToTerminal"
                  stroke="#82ca9d"
                  fill="#82ca9d"
                  name="DeliveredToTerminal"
                />
                <Area
                  type="monotone"
                  dataKey="exception"
                  stroke="#ffa500"
                  fill="#ffa500"
                  name="Exception sending "
                />
                <Area
                  type="monotone"
                  dataKey="invalid"
                  stroke="#ff0000"
                  fill="#ff0000"
                  name="InvalidMsisdn"
                />
                <Area
                  type="monotone"
                  dataKey="impossible"
                  stroke="#808080"
                  fill="#808080"
                  name="DeliveryImpossible"
                />
                <Area
                  type="monotone"
                  dataKey="absent"
                  stroke="#FF6347"
                  fill="#FF6347"
                  name="AbsentSubscriber"
                />
                  <Area
                  type="monotone"
                  dataKey="retried"
                  stroke="#8884d8"
                  fill="#8884d8"
                  name="SENT_RETRIED"
                />

                <Legend
                  verticalAlign="top"
                  height={36}
                  onClick={handleLegendClick}
                />
              </AreaChart>
            )}
          </ResponsiveContainer>
        </div>
      </div>
     
    </>
  );
};

export default DashTimeseries;
