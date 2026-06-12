import React, { useEffect } from "react";
import smsSvg from "../../assets/svg/sms.svg";
import ussdSvg from "../../assets/svg/svg24.svg";
import shortcodeSvg from "../../assets/svg/shortcodeSvg.svg";

import { Button, Tooltip } from "antd";
import Tip from "./Tip";
import { useDispatch } from "react-redux";
import { cleanRequestType, setRequestType } from "../../features/sms-request/smsRequestSlice";

function Product({ next, prev }) {
  const dispatch = useDispatch()
  const prodItems = [
    {
      id:"SMS",
      title: "SMS Sender Id",
      subTitle: "e.g. AbcLimited",
      desc: "Integrate our Bulk SMS API into your service to send branded messages to multiple numbers instanty.",
      tip: "Required documents",
      image:smsSvg
    },
    {
      id:'SHORTCODE',
      title: "Two-Way SMS Shortcode",
      subTitle: "e.g. 123456",
      desc: "Open a two-way line of commnication with your users and share information or collect feedback from users.",
      tip: "Required documents",
      image:shortcodeSvg
    },
    {
      id:'USSD',
      title: "USSD Service Code",
      subTitle: "e.g. *200#",
      desc: "USSD is a service that allows mobile phone users to interact with a remote application from their device in real time.",
      tip: "Required documents",
      image:ussdSvg
    },
  
     
  ];

  const handleClick = async(item) => { 
    await dispatch(setRequestType(item?.id)) 
    next();
  };

  // function clean(){
  //   dispatch(cleanRequestType())
  // }

  // useEffect(()=>{
  //   clean()
  // },[])

  return (
    <div className="lg:px-0 px-3">
      <div className="product_title flex justify-center items-center">
        Please select a product request below to get started:
      </div>
      <div className="mt-10 mb-10 flex flex-shrink flex-wrap w-full gap-y-[1rem] gap-x-[1rem]">
        {prodItems?.length > 0 &&
          prodItems?.map((item) => (
            <div
              className="product-card py-10 lg:w-[300px] w-full flex flex-col items-center justify-center cursor-pointer"
              onClick={()=>handleClick(item)}
            >
              <div className="product_title ">{item?.title}</div>
              <div className="product_sub mt-2">{item?.subTitle}</div>

              <div className="mt-10 mb-5 w-[2.5rem] h-[2.5rem] rounded-full flex items-center justify-center border border-[#cccccc]">
                <img src={item?.image} alt="sms" />
              </div>

              <div className="product_sub mt-5 flex items-center text-center justify-center px-5">
                {item?.desc}
              </div>

              <div className="product_sub mt-8 px-5 flex items-center text-center justify-center">
                <Tooltip placement="top" title={<Tip />}>
                  <span>Tip: Hover over me to see required documents.</span>
                </Tooltip>
              </div>
            </div>
          ))}
      </div>
    </div>
  );
}

export default Product;
