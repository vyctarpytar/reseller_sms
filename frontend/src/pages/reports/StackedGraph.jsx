import { Dropdown, Select } from "antd";
import React, { useState } from "react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";
import svg27 from "../../assets/svg/svg27.svg";

const data = [
  {
    name: "Page A",
    uv: 4000,
    pv: 2400,
    amt: 2400,
  },
  {
    name: "Page B",
    uv: 3000,
    pv: 1398,
    amt: 2210,
  },
  {
    name: "Page C",
    uv: 2000,
    pv: 9800,
    amt: 2290,
  },
  {
    name: "Page D",
    uv: 2780,
    pv: 3908,
    amt: 2000,
  },
  {
    name: "Page E",
    uv: 1890,
    pv: 4800,
    amt: 2181,
  },
  {
    name: "Page F",
    uv: 2390,
    pv: 3800,
    amt: 2500,
  },
  {
    name: "Page G",
    uv: 3490,
    pv: 4300,
    amt: 2100,
  },
];

const StackedGraph = () => {
  const [formData, setFormData] = useState();
  function handleSelectChange(value, formName) {
    setFormData((prevData) => ({
      ...prevData,
      [formName]: value,
    }));
  }

  const settingItems = [
    {
      key: "0",
      label: (
        <div className="font-dmSans text-black font-[400] text-[16px]">
          Download Report
        </div>
      ),
    },
  ];

  return (
    <div className="report-card w-full lg:h-[45vh] h-full lg:p-10 p-3 bg-white">
      <div className="flex lg:flex-row flex-col lg:items-center lg:justify-between">
        <div className="product_request_title !text-[31px]">
          SMS - By Status
        </div>
        <div className="justify-end w-[250px] flex items-center gap-x-5">
          <Select
            name="accStatus"
            value={formData?.accStatus}
            allowClear
            placeholder="Select Status"
            style={{
              width: "100%",
            }}
            onChange={(value) => {
              handleSelectChange(value, "accStatus");
            }}
            // options={
            //   accountStatusData?.length > 0 &&
            //   accountStatusData?.map((item) => ({
            //     value: item?.acName,
            //     label: item?.acName,
            //   }))
            // }
            showSearch
            optionFilterProp="children"
            filterOption={(input, option) =>
              (option?.label?.toLocaleLowerCase() ?? "").includes(
                input?.toLocaleLowerCase()
              )
            }
            onDropdownVisibleChange={() => {}}
          />
          <Dropdown
            overlayStyle={{
              width: "auto",
            }}
            trigger={"click"}
            menu={{ items: settingItems }}
            placement="bottom"
          >
            <img src={svg27} alt="svg27" />
          </Dropdown>
        </div>
      </div>

      <ResponsiveContainer width="100%" height="100%">
        <BarChart
          layout="vertical"
          width={500}
          height={300}
          data={data}
          margin={{
            top: 20,
            right: 30,
            left: 20,
            bottom: 5,
          }}
        >
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis type="number" />
          <YAxis type="category" dataKey="name" />
          <Tooltip />
          <Legend />
          <Bar dataKey="pv" stackId="a" fill="#8884d8" />
          <Bar dataKey="uv" stackId="a" fill="#82ca9d" />
          
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
};

export default StackedGraph;
