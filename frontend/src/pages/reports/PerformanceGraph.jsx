import { Dropdown, Select } from "antd";
import React, { useState } from "react";
import {
  BarChart,
  Bar,
  Rectangle,
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
    amt: 2400,
  },
  {
    name: "Page B",
    uv: 3000, 
    amt: 2210,
  },
  {
    name: "Page C",
    uv: 2000, 
    amt: 2290,
  },
  {
    name: "Page D",
    uv: 2780, 
    amt: 2000,
  },
  {
    name: "Page E",
    uv: 1890, 
    amt: 2181,
  },
  {
    name: "Page F",
    uv: 2390, 
    amt: 2500,
  },
  {
    name: "Page G",
    uv: 3490, 
    amt: 2100,
  },
];

const PerformanceGraph = () => {
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
          width={500}
          height={300}
          data={data}
          margin={{
            top: 5,
            right: 30,
            left: 20,
            bottom: 5,
          }}
          barSize={20}
        >
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="name" />
          <YAxis />
          <Tooltip />
          <Legend />
          <Bar
            dataKey="uv"
            fill="#82ca9d"
            radius={[10, 10, 0, 0]}
          />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
};

export default PerformanceGraph;
