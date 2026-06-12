import React, { useEffect, useState } from "react";
import InsideHeader from "../../components/InsideHeader";
import CreditTable from "./CreditTable";
import svg30 from "../../assets/svg/svg30.svg";
import CreditAddModal from "./CreditAddModal";
import CreditAddSelfModal from "./CreditAddSelfModal";
import { useDispatch, useSelector } from "react-redux";
import FilterModal from "./FilterModal";
import { fetchCreditAccount, fetchCreditAdmin, fetchCreditReseller } from "../../features/credit/creditSlice";
import svg38 from "../../assets/svg/svg38.svg";
import MaterialIcon from "material-icons-react";

function CreditList() {
  // const dispatch = useDispatch();
  // const [isModalOpen, setIsModalOpen] = useState(false);
  // const showModal = () => {
  //   setIsModalOpen(true);
  // };
  // const { user } = useSelector((state) => state.auth);
  // const [isModalOpenSelf, setIsModalOpenSelf] = useState(false);
  // const showModalSelf = () => {
  //   setIsModalOpenSelf(true);
  // };
  // const [prodd, setProdd] = useState();
  // const { resellerId } = useSelector((state) => state.global);
  // const [isModalOpenFilter, setIsModalOpenFilter] = useState(false);
  // const showModalFilter = () => {
  //   setIsModalOpenFilter(true);
  // };
  // const [formData, setFormData] = useState({});

  // const handleClearFilters = async () => {
  //   // await setFormData({});
  //   // const res = await dispatch(
  //   //   fetchMyUsers({
  //   //     url: "api/v2/users/get",
  //   //     firstname: null,
  //   //     lastname: null,
  //   //     email: null,
  //   //     phoneNumber: null,
  //   //     usrNationalId: null,
  //   //     usrStatus: null,
  //   //     role: null,
  //   //   })
  //   // );
  // };

  // async function fetchCreditData(page, size) {
  //   if(user?.layer === "RESELLER"){ 
  //   const res = await dispatch(
  //     fetchCreditReseller({
  //       url: "api/v2/credit/reseller-account",
  //       limit: size ?? pageSize,
  //       start: page ?? pageIndex,
  //       smsResellerName: formData?.smsResellerName,
  //       smsAccountName: formData?.smsAccountName,
  //       smsCreatedByName: formData?.smsCreatedByName,
  //       smsCreatedDate: formData?.smsCreatedDate,
  //       smsApprovedByName: formData?.smsApprovedByName,
  //       smsApprovedDate: formData?.smsApprovedDate,
  //       crStatus: formData?.crStatus,
  //     })
  //   );
  // }
  // }


  // async function fetchCreditData() {
  //   if(user?.layer === "ACCOUNT"){
  //     const res = await dispatch(
  //       fetchCreditAccount()
  //     );
  //   }

  //   if(user?.layer === "RESELLER"){ 
  //       const res = await dispatch(
  //         fetchCreditReseller({
  //           accId:resellerId
  //         })
  //       ); 
  //   }
  //   if(user?.layer === "TOP"){
  //     const res = await dispatch(
  //       fetchCreditAdmin()
  //     );
  //   } 
  // }

  // useEffect(() => {
  //   fetchCreditData();
  // }, []); 
  return (
    <div className="w-full overflow-y-scroll h-full">
      <InsideHeader
        title="Credit Analysis"
        subtitle="Load and manage credit for your resellers"
        back={false}
      />

      <div className="lg:px-10 px-3">
        <div className="mt-10 product_request_title !text-[31px]">
          SMS - Bulk Credit
        </div>

        <div className="mt-[1.31rem] ">
          {/* <div className=" flex items-center gap-x-5">
            <div className="w-[250px]">
              <button
                className="cstm-btn !rounded-[4px] !bg-[#A3A2A7] !text-[.75rem] flex items-center gap-x-3"
                onClick={showModal}
              >
                <img src={svg30} alt="svg30" />
                Load Account Credit
              </button>
            </div>
            {user?.layer === "RESELLER" && (
              <div className="w-[250px]">
                <button
                  className="cstm-btn !rounded-[4px] !bg-[#A3A2A7] !text-[.75rem] flex items-center gap-x-3"
                  onClick={showModalSelf}
                >
                  <img src={svg30} alt="svg30" />
                  Load Self Credit
                </button>
              </div>
            )}
              <div className="flex items-center">
            <span>
              {" "}
              <button
                onClick={showModalFilter}
                type="button"
                className={`bg-transparent flex items-center gap-x-'1' ${
                  Object?.keys(formData)?.length > 0
                    ? "!text-[#5688E5]"
                    : "inherit"
                }`}
              >
                <MaterialIcon color="#141414" icon="filter_list" />
                Filters
              </button>
            </span>
            {Object?.keys(formData)?.length > 0 && (
              <span className="flex items-center text-[#5688E5] cursor-pointer ml-1">
                :{Object?.keys(formData)?.length}
                <img src={svg38} alt="svg38" onClick={handleClearFilters} />
              </span>
            )}
          </div>
          </div> */}

          <CreditTable  />
        </div>
      </div>
      {/* <CreditAddModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        prodd={prodd}
      />

      <CreditAddSelfModal
        isModalOpen={isModalOpenSelf}
        setIsModalOpen={setIsModalOpenSelf}
        prodd={prodd}
      />

      <FilterModal
        isModalOpen={isModalOpenFilter}
        setIsModalOpen={setIsModalOpenFilter}
        formData={formData}
        setFormData={setFormData}
      /> */}
    </div>
  );
}

export default CreditList;
