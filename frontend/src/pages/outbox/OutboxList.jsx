import React from "react";
import InsideHeader from "../../components/InsideHeader";
import { useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";
import svg23 from "../../assets/svg/svg23.svg";
import svg6 from "../../assets/svg/svg6.svg";
import svg31 from "../../assets/svg/svg31.svg";

function OutboxList() {
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const handleSingleSms = () => {
    navigate("/group-individual-list")
  };
  const handleBulkSms = () => {
    navigate("/group");
  };
  const handleExcelSms = () => {
    navigate("/excel-sms");
  };
  const handleDynamicSms = () => {
    navigate("/dynamic-table");
  };
  

  return (
    <div className="w-full h-full overflow-y-scroll">
      <InsideHeader
        title="Sms Outbox"
        subtitle="Message and view your sms logs here"
        back={false}
      />

      <div className="lg:px-10 px-3">
        <div className="flex flex-col mt-10">
          <div className="product_request_title !text-[31px]">
            SMS - Bulk Outbox
          </div>
          <div className="mt-5 paragraph">
            This is a log of your bulk sms outbox.
          </div>
        </div>

        <div className="mt-[1rem]  flex flex-shrink flex-wrap w-full gap-y-[1rem] gap-x-[1rem]">
          <div onClick={handleSingleSms} className="cursor-pointer">
            <div className="product-card  min-h-[6.125rem] lg:w-[550px] w-full">
              <div className="flex justify-between p-[.875rem] items-center">
                <div className="flex items-center gap-x-5">
                  <div className="w-[3.875rem] h-[3.875rem] flex justify-center items-center bg-darkGreen rounded-full">
                    <img src={svg31} alt={svg31} />
                  </div>

                  <div className="reseller_card_title">Single SMS</div>
                </div>

                <div className="flex-end">
                  <img src={svg6} alt="svg6" />
                </div>
              </div>

              <div className="bg-[#f4f4f5] p-[.875rem] flex flex-col">
                <div className="reseller_card_title !text-[16px]">
                  Single SMS
                </div>
                <div className="reseller_card_sub_title">
                  This option is good for single sms to a specific customer
                </div>
              </div>
            </div>
          </div>

          <div onClick={handleBulkSms} className="cursor-pointer">
            <div className="product-card  min-h-[6.125rem] lg:w-[550px] w-full">
              <div className="flex justify-between p-[.875rem] items-center">
                <div className="flex items-center gap-x-5">
                  <div className="w-[3.875rem] h-[3.875rem] flex justify-center items-center bg-darkGreen rounded-full">
                    <img src={svg23} alt={svg23} />
                  </div>

                  <div className="reseller_card_title">Bulk SMS</div>
                </div>

                <div className="flex-end">
                  <img src={svg6} alt="svg6" />
                </div>
              </div>

              <div className="bg-[#f4f4f5] p-[.875rem] flex flex-col">
                <div className="reseller_card_title !text-[16px]">Bulk SMS</div>
                <div className="reseller_card_sub_title">
                  This option is good for bulk sms to multiple customers
                </div>
              </div>
            </div>
          </div>

          <div onClick={handleExcelSms} className="cursor-pointer">
            <div className="product-card  min-h-[6.125rem] lg:w-[550px] w-full">
              <div className="flex justify-between p-[.875rem] items-center">
                <div className="flex items-center gap-x-5">
                  <div className="w-[3.875rem] h-[3.875rem] flex justify-center items-center bg-darkGreen rounded-full">
                    <img src={svg23} alt={svg23} />
                  </div>

                  <div className="reseller_card_title">Upload Excel SMS</div>
                </div>

                <div className="flex-end">
                  <img src={svg6} alt="svg6" />
                </div>
              </div>

              <div className="bg-[#f4f4f5] p-[.875rem] flex flex-col">
                <div className="reseller_card_title !text-[16px]">Excel SMS</div>
                <div className="reseller_card_sub_title">
                  This option is good when you have ready excel with customer mobile and message
                </div>
              </div>
            </div>
          </div>


          <div onClick={handleDynamicSms} className="cursor-pointer">
            <div className="product-card  min-h-[6.125rem] lg:w-[550px] w-full">
              <div className="flex justify-between p-[.875rem] items-center">
                <div className="flex items-center gap-x-5">
                  <div className="w-[3.875rem] h-[3.875rem] flex justify-center items-center bg-darkGreen rounded-full">
                    <img src={svg23} alt={svg23} />
                  </div>

                  <div className="reseller_card_title">Campaign SMS</div>
                </div>

                <div className="flex-end">
                  <img src={svg6} alt="svg6" />
                </div>
              </div>

              <div className="bg-[#f4f4f5] p-[.875rem] flex flex-col">
                <div className="reseller_card_title !text-[16px]">Campaign SMS</div>
                <div className="reseller_card_sub_title">
                  This option is good when you want to send one time message to multiple contacts
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default OutboxList;
