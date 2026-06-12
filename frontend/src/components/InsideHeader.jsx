import React from "react";
import { useNavigate } from "react-router-dom";

function InsideHeader({ title, subtitle, back,handleGoBack }) {
  const navigate = useNavigate();
  const handleGoBackDefault = () => {
    navigate(-1);
  };
  return (
    <> 
      <div className="w-[100%] h-auto bg-lightBlue  py-[25px] px-10 lg:flex hidden justify-between items-center">
        <div className="flex flex-col">
          <span className="font-bold text-[40px] text-[#141414] dash-title leading-[60.75px]">
            {title}
          </span>
          <span className="text-black21 text-[18px] font-normal leading-[24px] font-dmSans">
            {subtitle}
          </span>
        </div>

        {back && (
          <div className="flex items-center">
            <button
              type="button"
              onClick={handleGoBack ? handleGoBack : handleGoBackDefault}
              className="bg-transparent flex items-center"
            >
              <div className="flex justify-center items-center rounded-full w-[40px] h-[40px] p-2 bg-[#EDF8FF]">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="28"
                  height="24"
                  viewBox="0 0 28 24"
                  fill="none"
                >
                  <path
                    d="M17.7692 18L11.0469 12L17.7692 6"
                    stroke="#147CBC"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
              </div>
              <span className="font-bold text-[22px] text-[#222222] font-dmSans">
                Go Back
              </span>
            </button>
          </div>
        )}
      </div> 
    </>
  );
}

export default InsideHeader;
