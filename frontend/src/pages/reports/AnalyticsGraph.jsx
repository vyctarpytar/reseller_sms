import { Dropdown, Select } from 'antd';
import React, { useState } from 'react';
import { PieChart, Pie, Legend, Tooltip, ResponsiveContainer } from 'recharts';
import svg27 from "../../assets/svg/svg27.svg";
import { useSelector } from 'react-redux';
 
const data02 = [
    { name: 'Group A', value: 2400, fill: '#8884d8' },
    { name: 'Group B', value: 4567, fill: '#8dd1e1' },
    { name: 'Group C', value: 1398, fill: '#82ca9d' },
    { name: 'Group D', value: 9800, fill: '#a4de6c' },
    { name: 'Group E', value: 3908, fill: '#d0ed57' },
    { name: 'Group F', value: 4800, fill: '#ffc658' },
  ];


 
  
const AnalyticsGraph = () => {
  const { report, loading } = useSelector((state) => state.dash);
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
      <PieChart width={400} height={400} className='pie-legend'> 
        <Pie dataKey="value" data={data02} innerRadius={50} outerRadius={85}   />
        <Legend  />
        <Tooltip /> 
      </PieChart> 
    </ResponsiveContainer>
    </div>
 
  );
};

export default AnalyticsGraph;
