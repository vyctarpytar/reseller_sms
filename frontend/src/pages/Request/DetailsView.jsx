import React, { useEffect, useState } from "react";
import svg28 from "../../assets/svg/svg28.svg";
import svg29 from "../../assets/svg/svg29.svg";
import { Progress } from "antd";
import { useParams } from "react-router-dom";
import { fetchSingleRequest } from "../../features/product-request/productRequestSlice";
import { useDispatch, useSelector } from "react-redux";
import InitiateModal from "./InitiateModal";
import { addSpaces, dateForHumans, formatPath } from "../../utils";
import ConfirmSubmitModal from "../../components/ConfirmSubmitModal";
import { save } from "../../features/save/saveSlice";
import toast from "react-hot-toast";
import { downloadFiles } from "../../features/global/globalSlice";

function DetailsView() {
  const { id } = useParams();
  const dispatch = useDispatch();
  const { singleRequest } = useSelector((state) => state.productRequest);
  const { user } = useSelector((state) => state.auth);
  const { saving } = useSelector((state) => state.save);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };

  const [isModalOpenConfirmation, setIsModalOpenConfirmation] = useState(false);
  const showModalConfirmation = async () => {
    setIsModalOpenConfirmation(true);
  };

  const handleSubmit = async () => {
    const res = await dispatch(
      save({
        url: `api/v2/req/trigget-in-progress/${id}`,
      })
    );
    if (res?.payload?.success) {
     await toast.success(res?.payload?.messages?.message);
     await fetchSingleRequestData();
      setIsModalOpenConfirmation(false)
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  function fetchSingleRequestData() {
    dispatch(
      fetchSingleRequest({
        reqId: id,
      })
    );
  }
  useEffect(() => {
    fetchSingleRequestData();
  }, [id]);

  const handleClick = async (item) => {  
    const res = await dispatch(downloadFiles({
      fileName:item
    }));

    if (res?.payload) { 
      const blob = new Blob([res.payload], {
        type: "application/octet-stream",
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", formatPath(item));  
      document.body.appendChild(link);
      link.click();

 
      link.parentNode.removeChild(link);

      toast.success("File downloaded successfully");
    } else {
      toast.error("Failed to download file");
    }
  };

  return (
    <div className="flex lg:flex-row flex-col gap-x-10 mb-10">
      <div className="lg:w-[70%] w-[100%]">
        <div className="prod_view_subtitle">Service Details</div>

        <div className="mt-[0.5rem] border border-[#f1f1f1] rounded-[3px] p-3 ">
          <div className="grid grid-cols-2 prod_view_table gap-y-[1rem] prod_view_table_css">
            <span>Country</span>
            <span>{singleRequest?.raCountry ?? "Kenya"}</span>

            <span>Telcos</span>
            <span>{singleRequest?.reTelcos ?? null}</span>

            <span>Description</span>
            <span>{singleRequest?.reDesc ?? null}</span>

            <span>Permission Letter</span>
            <span className="flex items-center text-darkGreen cursor-pointer"
                onClick={()=>handleClick(singleRequest?.reAuthorizationFileName)}>
              <img src={svg28} alt="svg28" />
              {formatPath(singleRequest?.reAuthorizationFileName)}
            </span>
          </div>
        </div>

        <div className="prod_view_subtitle mt-10">Contact Details</div>
        <div className="prod_view_table pt-8">
          Type: {singleRequest?.reseller?.rsBusinessType}
        </div>

        <div className="prod_view_subtitle mt-10">Company Details</div>
        <div className="mt-[0.5rem] border border-[#f1f1f1] rounded-[3px] p-3 ">
          <div className="grid grid-cols-2 prod_view_table gap-y-[1rem] prod_view_table_css">
            <span>Company name</span>
            <span>{singleRequest?.reseller?.rsCompanyName}</span>

            <span>Company Domain</span>
            <span>{singleRequest?.reseller?.rsDomain}</span>
            

            <span>Industry</span>
            <span>{singleRequest?.reseller?.reIndustry ?? null}</span>

            <span>Physical Address</span>
            <span>{singleRequest?.reseller?.reAddress ?? null}</span>

            <span>Country</span>
            <span>{singleRequest?.reseller?.raCountry ?? "Kenya"}</span>

            <span>Postal Address</span>
            <span>{singleRequest?.reseller?.raPostalCode}</span>

            <span>Tax Number</span>
            <span>{singleRequest?.reseller?.vatNumber ?? null}</span>

            <span>Certificate of Incorporation</span>
            <span className="flex items-center text-darkGreen cursor-pointer"
                onClick={()=>handleClick(singleRequest?.reIncorporationCertFileName)}>
              <img src={svg28} alt="svg28" />
              {formatPath(singleRequest?.reIncorporationCertFileName)}
            </span>

            <span>Tax Certificate</span>
            <span className="flex items-center text-darkGreen cursor-pointer"
                onClick={()=>handleClick(singleRequest?.reKraFileName)}>
              <img src={svg28} alt="svg28" />
              {formatPath(singleRequest?.reKraFileName)}
            </span>
          </div>
        </div>

        <div className="prod_view_subtitle mt-10">Contact Person</div>

        <div className="mt-[0.5rem] border border-[#f1f1f1] rounded-[3px] p-3 ">
          <div className="grid grid-cols-2 prod_view_table gap-y-[1rem] prod_view_table_css">
            <span>Full Legal Name</span>
            <span>{singleRequest?.reseller?.rsContactPerson ?? null}</span>

            <span>Email Address</span>
            <span>{singleRequest?.reseller?.rsEmail ?? null}</span>

            <span>Mobile No.</span>
            <span>
              +{addSpaces(singleRequest?.reseller?.rsPhoneNumber ?? null)}
            </span>

            <span>Office Telephone No.</span>
            <span>
              +{addSpaces(singleRequest?.reseller?.rsPhoneNumber ?? null)}
            </span>
          </div>
        </div>
      </div>

      <div className="lg:w-[30%] w-[100%] flex flex-col">
        <div className="prod_view_subtitle">Details</div>

        <div className="prod_view_table !text-[16px] mt-[1rem]">
          Reference No.
        </div>
        <div className="prod_view_table !text-[#808080]">
          {singleRequest?.reResellerId ?? null}
        </div>

        <div className="prod_view_table !text-[16px] mt-[1rem]">Status</div>
        <div
          className={`rounded-[1rem] p-[.25rem] ${
            singleRequest?.reStatus == "PROCESSED"
              ? "bg-[#388E3C]"
              : "bg-[#808080]"
          }  flex items-center justify-center !text-white
           w-[220px] gap-x-[5px] prod_view_table !text-[16px]`}
        >
          <img src={svg29} alt="29" />
          {singleRequest?.reStatus == "PENDING"
            ? "Pending"
            : singleRequest?.reStatus == "PROCESSING"
            ? "Submitted for Review"
            : "Completed"}
        </div>

        <div className="prod_view_table !text-[16px] mt-[1.5rem]">Progress</div>
        <div>
          {" "}
          <Progress percent={30} />
        </div>

        <div className="prod_view_table !text-[16px] mt-[1rem]">
          Last Updated
        </div>
        <div className="prod_view_table !text-[16px]  ">
          {dateForHumans(singleRequest?.reCreatedDate)}
        </div>

        {(singleRequest?.reStatus == "PROCESSING" && user?.layer === "TOP") && (
          <>
            <div className="prod_view_subtitle mt-[2rem]">Actions</div>
            <div className="w-[220px] !text-[18px]">
              <button className="cstm-btn" onClick={showModal}>
               Issue Sender ID
              </button>
            </div>
          </>
        )}
        {(singleRequest?.reStatus == "PENDING" && user?.layer === "TOP") && (
          <>
            <div className="prod_view_subtitle mt-[2rem]">Actions</div>
            <div className="w-[200px] !text-[18px]">
              <button className="cstm-btn" onClick={showModalConfirmation}>
                Send in Progress
              </button>
            </div>
          </>
        )}

 
      </div>

      <InitiateModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        id={id}
      />

      <ConfirmSubmitModal
        isModalOpen={isModalOpenConfirmation}
        setIsModalOpen={setIsModalOpenConfirmation}
        content="Are you sure?"
        subContent={`Are you sure that you want to send in progress`}
        handleSubmit={handleSubmit}
        loading={saving}
      />
    </div>
  );
}

export default DetailsView;
