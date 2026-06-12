import React, { useEffect } from "react";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";
import { fetchPaymentHistory } from "../../features/billing/billingSlice";
import { useDispatch, useSelector } from "react-redux";

 

const PaymentHistory = () => {
  const dispatch = useDispatch();
  async function fetchPaymentHistoryData() {
    dispatch(fetchPaymentHistory());
  }

  const { paymentHistoryData } = useSelector((state) => state.billing);

  useEffect(() => {
    fetchPaymentHistoryData();
  }, []);

 
  return (
    <div className="report-card w-full lg:h-[56vh] h-full pt-2 pb-10 p-3 bg-white">
      <div className="product_sub !text-[18px] mb-5">Earnings History</div>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart
          width={500}
          height={300}
          data={paymentHistoryData}
          margin={{
            top: 5,
            right: 30,
            left: 20,
            bottom: 5,
          }}
        >
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="monthName" />
          <YAxis dataKey="amount"  />
          <Tooltip />
          <Legend />
          <Line
            type="monotone"
            dataKey="amount"
            stroke="#82ca9d"
            activeDot={{ r: 8 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default PaymentHistory;
