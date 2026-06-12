import React, { useEffect } from "react";
import InsideHeader from "../../components/InsideHeader";
import { useDispatch, useSelector } from "react-redux";
import { fetchMyProfile } from "../../features/auth/authSlice";
import svg23 from "../../assets/svg/svg23.svg";
import svg42 from "../../assets/svg/svg42.svg";

const prodItems = [
  {
    id: "1",
    title: "Current Collection Balance",
    money: "220",
    desc: " 21212 - Main Wallet",
    image: svg42,
    color:'#C93C26'
  },
  {
    id: "2",
    title: "Today's Total Collection",
    money: "30",  
    color:'#1D191B'
  },
  {
    id: "3",
    title: "Monthly Total Collection",
    money: "3",  
    color:'#7C888B'
  },
  {
    id: "4",
    title: "Current Disbursement Balance",
    money: "0",  
    color:'#F4B940'
  },
  {
    id: "5",
    title: "Today's Total Disbursement",
    money: "70",  
    color:'#1D191B'
  },
  {
    id: "6",
    title: "Monthly Total Disbursement",
    money: "300",  
    color:'#7C888B'
  },
];

function Billing() {
  const dispatch = useDispatch();
  const { userProfile } = useSelector((state) => state.auth);

  async function fetchMyUsersData() {
    await dispatch(fetchMyProfile());
  }

  useEffect(() => {
    fetchMyUsersData();
  }, []);
  return (
    <div className="w-full overflow-y-scroll h-full">
      <InsideHeader
        title="Billing"
        subtitle="Create and manage your bills here."
        back={false}
      />

      <div className="mt-10 flex flex-col lg:px-10 px-3">
        <div className="product_request_title !text-[31px]">
          <span className="text-darkGreen">Welcome, </span>
          {userProfile?.firstname}
        </div>
        <div className="product_sub mt-[0.5rem]">
          Here is a summary of your payment activity
        </div>

        <div className="mt-[1rem] flex flex-shrink flex-wrap w-full gap-y-[1rem] gap-x-[1rem]">
          {prodItems?.length > 0 &&
            prodItems?.map((item) => (
              <div className={`billing-card min-h-[7.125rem] lg:w-[450px] w-full p-5 cursor-pointer 
              flex flex-col gap-y-[1px]`}
              style={{ backgroundColor: item?.color }}>
                <div className="reseller_card_title !text-white">
                 {item?.title}
                </div>
                <div className="reseller_card_sub_title !text-white">KES {item?.money}</div>
                <div className="flex items-center reseller_card_sub_title gap-x-2 !text-white">
                 {item?.image && <img src={item?.image} alt="svg42" />} 
                  {item?.desc} 
                </div>
              </div>
            ))}
        </div>
      </div>
    </div>
  );
}

export default Billing;
