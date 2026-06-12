import React, { useEffect, useRef, useState } from "react";
import { Form, Steps, Button, theme, message } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import Product from "./Product";
import ProductInfo from "./ProductInfo";
import ContactInfo from "./ContactInfo";
import {
  cleanCurrent,
  setCurrent,
} from "../../features/sms-request/smsRequestSlice";
import InsideHeader from "../../components/InsideHeader";
import UssdInfo from "./UssdInfo";
import ShortCodeInfo from "./ShortCodeInfo";

const RequestList = () => {
  const [form] = Form.useForm();
  const formRef = useRef(null);

  const dispatch = useDispatch();

  const { current, requestType } = useSelector((state) => state.sms);

  const { token } = theme.useToken();
  // const [current, setCurrent] = useState(0);
  const next = () => {
    dispatch(setCurrent(current + 1));
  };
  const prev = () => {
    dispatch(setCurrent(current - 1));
  };

  const steps = [
    {
      title: "Select product",
      content: <Product next={next} prev={prev} />,
      icon: (
        <span className="w-[24px] h-[24px] rounded-full border-2 border-[#D1D5DB]"></span>
      ),
    },

    {
      title: "Product Info",
      content:
        requestType === "SMS" ? (
          <ProductInfo next={next} prev={prev} />
        ) : requestType === "USSD" ? (
          <UssdInfo next={next} prev={prev} />
        ) : requestType === "SHORTCODE" ? (
          <ShortCodeInfo next={next} prev={prev} />
        ) : null,
      icon: (
        <span className="w-[24px] h-[24px] rounded-full border-2 border-[#D1D5DB]"></span>
      ),
    },
  ];

  const customDot = (dot, { status, index }) => {
    let icon;
    if (status === "process") {
      icon = (
        <span className="w-[24px] h-[24px] rounded-full border-2 border-[#14B04C] flex items-center justify-center">
          <div className="w-[10px] h-[10px] rounded-full bg-[#14B04C]"></div>
        </span>
      );
    } else if (status === "finish") {
      icon = (
        <span className="w-[24px] h-[24px] rounded-full border-2 border-[#14B04C] flex items-center justify-center">
          <div className="w-[10px] h-[10px] rounded-full bg-[#14B04C]"></div>
        </span>
      );
    } else {
      icon = steps[index].icon;
    }

    return (
      <div className="custom-step">
        {icon}
        <div className="custom-step-title">{steps[index].title}</div>
      </div>
    );
  };

  const items = steps.map((item) => ({
    key: item.title,
    title: "",
  }));

  const contentStyle = {
    // textAlign: "center",
    color: token.colorTextTertiary, 
  };

  return (
   <div className="w-full h-full overflow-y-scroll">
      <InsideHeader
        title="Sms Request"
        subtitle="Make all your requests here"
        back={false}
      />  
      <div className="w-full flex justify-center items-center">
        <div className="flex flex-col mt-10 lg:w-[70%] w-full"> 
            <Steps
              current={current}
              progressDot={customDot}
              items={items}
              className=""
            />
            <div className style={contentStyle}>{steps[current].content}</div> 
        </div>
      </div> 
    </div>
  );
};

export default RequestList;
