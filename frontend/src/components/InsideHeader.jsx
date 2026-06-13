import React from "react";
import { useNavigate } from "react-router-dom";

function InsideHeader({ title, subtitle, back,handleGoBack }) {
  const navigate = useNavigate();
  const handleGoBackDefault = () => {
    navigate(-1);
  };
  return (
    <> 
      <div className="w-[100%] h-auto bg-white border-b border-[#ECE9E4] pt-4 lg:pt-7 pb-4 px-4 lg:px-10 flex justify-between items-end">
        <div className="flex items-center gap-x-4">
          {back && (
            <button
              type="button"
              onClick={handleGoBack ? handleGoBack : handleGoBackDefault}
              className="bg-transparent flex justify-center items-center rounded-full w-[38px] h-[38px] border border-[#ECE9E4] hover:bg-[#F7F5F2] transition-colors"
              aria-label="Go back"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 28 24" fill="none">
                <path
                  d="M17.7692 18L11.0469 12L17.7692 6"
                  stroke="#69472E"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </button>
          )}
          <div className="flex flex-col gap-1">
            <span className="font-bold text-[22px] lg:text-[28px] text-[#1A1A1A] dash-title leading-tight tracking-tight">
              {title}
            </span>
            {subtitle && (
              <span className="text-[#8A8178] text-[15px] font-normal leading-[20px] font-dmSans">
                {subtitle}
              </span>
            )}
          </div>
        </div>
      </div>
    </>
  );
}

export default InsideHeader;
