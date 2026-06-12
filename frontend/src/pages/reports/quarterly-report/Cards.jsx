import React from "react";
import { useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom"; 
import svg53 from "../../../assets/svg/svg53.svg";
import svg54 from "../../../assets/svg/svg54.svg";
import svg55 from "../../../assets/svg/svg55.svg"; 
import svg56 from "../../../assets/svg/svg56.svg";
import svg57 from "../../../assets/svg/svg57.svg";
import svg58 from "../../../assets/svg/svg58.svg"; 

function Cards({ dataCard, }) {
  const navigate = useNavigate();
  const dispatch = useDispatch();
 
   const svgMap = {
    svg53,
    svg54,
    svg55,
    svg56,
    svg57,
    svg58
  };
  const processedData = dataCard && dataCard?.map((item) => ({
    ...item,
   svg: svgMap[item.svg],
  }));

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mt-5">
      {processedData?.map((item, index) => (
        <div
          key={index}
          className="rounded-[.75rem] border border-[#D6E1F5] bg-[#F4F5F7] h-[6.25rem]   p-[.5rem]"
        >
          <div className="flex items-center justify-between">
            <div className="scheme-card-title text-sm font-semibold text-gray-700">
              {item?.title}
            </div>
           <img src={item?.svg} alt={item?.svg} />
          </div>
          <div className="scheme-card-content mt-[1.62rem] text-xl font-bold text-gray-900">
            {item?.value}
          </div>
        </div>
      ))}
    </div>
  );
}

export default Cards;
