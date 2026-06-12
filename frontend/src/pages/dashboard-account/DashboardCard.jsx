import React, { useEffect } from "react";
import smsSvg from "../../assets/svg/smsDash.svg"; 
import svg39 from "../../assets/svg/svg39.svg";
import { numberWithCommas } from "../../utils";

function DashboardCard({ dashData }) {  
  return (
    <div className="">
      <div className="mt-[.2rem] flex flex-shrink flex-wrap w-full gap-y-[1rem] gap-x-[1rem]">
        {dashData?.ststusSummary &&
          dashData?.ststusSummary?.map((item) => (
            <div className={`dash-card lg:w-[16.36975rem] w-full p-5 bg-white}`}>
              <div className="bg-blue h-[50px] w-[50px] rounded-[5px] flex items-center justify-center">
                <img src={smsSvg} alt="smsSvg" />
              </div>

              <div className="dash-card-title mt-3 ">{item?.msgStatus}</div>
              <div className="dash-card-total mt-3">{numberWithCommas(item?.msgCount)}</div>

              <div className="flex justify-between items-center mt-[-15px]">
                <div></div>
                <div className="dash-card-total !text-[10px] !text-darkGreen justify-end flex items-center">
                  {item?.msgPerCent}%
                  <span>
                    <img src={svg39} alt="svg39" />
                  </span>
                </div>
              </div>
            </div>
          ))}
      </div>
    </div>
  );
}

export default DashboardCard;
