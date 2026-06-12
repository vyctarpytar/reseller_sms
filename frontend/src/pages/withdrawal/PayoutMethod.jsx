import React, { useEffect } from "react";
import mpesa from "../../assets/svg/mpesa.jpeg";
import svg48 from "../../assets/svg/svg48.svg";
import paypal from "../../assets/svg/paypal.png";
import bitcoin from "../../assets/svg/bitcoin-btc-logo.png";

import { Divider, Table, Tag } from "antd";
import { fetchTariff } from "../../features/billing/billingSlice";
import { useDispatch, useSelector } from "react-redux";
import { cashConverter, numberWithCommas } from "../../utils";

const prodItems = [
  {
    id: "MPESA",
    title: "M-Pesa",
    image: mpesa,
  },
  {
    id: "BANK",
    title: "Bank",
    image: svg48,
  },
  {
    id: "PAYPAL",
    title: "PAYPAL",
    image: paypal,
  },
  {
    id: "BITCOIN",
    title: "Cryptocurrency",
    image: bitcoin,
  },
];

function PayoutMethod() {
  const dispatch = useDispatch();
  const { loading, tariffData } = useSelector((state) => state.billing);

  const newTariff =
    tariffData &&
    tariffData
      ?.filter((item) => item?.tarType === "MPESA")
      ?.sort((a, b) => a?.tarFrom - b?.tarFrom);

  async function fetchTariffData() {
    await dispatch(fetchTariff());
  }

  useEffect(() => {
    fetchTariffData();
  }, []);
  const columns = [
    {
      title: "From",
      render: (item) => {
        return <div>{numberWithCommas(item)}</div>;
      },
      dataIndex: "tarFrom",
    },
    {
      title: "To",
      render: (item) => {
        return <div>{numberWithCommas(item)}</div>;
      },
      dataIndex: "tarTo",
    },
    {
      title: "Type",
      render: (item) => {
        return <div>{item === "MPESA" ? "B2C" : null}</div>;
      },
      dataIndex: "tarType",
    },
    {
      title: "Charges",
      dataIndex: "tarCharges",
    },
  ];

  return (
    <div className="report-card px-3 p-2 h-auto w-full">
      <div className="product_sub !text-[18px]">Payout Tariff</div>
      <Table
        dataSource={newTariff}
        columns={columns}
        className="mt-[1.31rem] w-full"
        scroll={{
          x: 800,
        }}
        pagination={{
          position: ["bottomCenter"],
          pageSize: 5,
        }}
        loading={loading}
        rowKey={(record) => record?.tarId}
      />
      {/* {prodItems &&
        prodItems?.map((item) => (
          <>
            <div className="flex justify-between mt-5 items-center">
              <div className="flex items-center gap-x-3">
                <img
                  src={item?.image}
                  alt="mpesa"
                  className="w-[80px] h-full"
                />
                <span className="font-[500]">{item?.title}</span>
                {item?.title === "M-Pesa" && (
                  <span>
                    {" "}
                    <Tag className="bg-[rgb(166,204,245)] text-[#007BFF]">
                      Primary
                    </Tag>
                  </span>
                )}
              </div>
              <div className="lg:w-[150px] w-auto">
                <button
                  type="button"
                  className="cstm-btn !bg-white !text-[#555555] !rounded-[10px] !border !border-[#388E3C]"
                >
                  {item?.title === "M-Pesa" ? "Disconnect" : "Connect"}
                </button>
              </div>
            </div>
            <Divider />
          </>
        ))} */}
    </div>
  );
}

export default PayoutMethod;
