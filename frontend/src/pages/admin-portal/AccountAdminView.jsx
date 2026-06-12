import React, { useEffect, useRef, useState } from "react";
import svg28 from "../../assets/svg/svg28.svg";
import svg29 from "../../assets/svg/svg29.svg";
import { Form} from "antd";
import { useNavigate, useParams } from "react-router-dom"; 
import { useDispatch, useSelector } from "react-redux";
import { addSpaces,  formatPath } from "../../utils"; 
import toast from "react-hot-toast";
import ConfirmSubmitModal from "../../components/ConfirmSubmitModal";
import { save } from "../../features/save/saveSlice"; 
import { downloadFiles } from "../../features/global/globalSlice";
import InsideHeader from "../../components/InsideHeader"; 
import DisableModal from "./DisableModal";
import { fetchSingleAccount } from "../../features/reseller-account/resellerAccountSlice";

function AccountAdminView() {
  const { id } = useParams();
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { singleAcc } = useSelector((state) => state.resellerAccount); 
  const { saving } = useSelector((state) => state.save);
 
  const [form] = Form.useForm();
  const formRef = useRef(null);

  const [isModalOpenConfirmation, setIsModalOpenConfirmation] = useState(false);
  const showModalConfirmation = async () => {
    setIsModalOpenConfirmation(true);
  };

  const [isModalOpenDelete, setIsModalOpenDelete] = useState(false);
  const showModalDelete = async () => {
    setIsModalOpenDelete(true);
  };
  const handleDelete=()=>{
 
  }

  const [data, setData] = useState({});
  const [prodd, setProdd] = useState();
  const [formData, setFormData] = useState();
   
  function fetchSingleAccountData() {
    dispatch(
      fetchSingleAccount({
        accId: id,
      })
    );
  }
 
 
  const handleSubmit = async () => {
    const res = await dispatch(
      save({ 
        url: `api/v2/account/enable/${singleAcc?.accId}`,
      })
    );
    if (res?.payload?.success) {
      toast.success(res?.payload?.messages?.message);
      await navigate("/account-admin");
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  useEffect(() => {
    fetchSingleAccountData();
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
    <div className="w-full h-full overflow-y-scroll">
    <InsideHeader
        title={`${singleAcc?.accName}`}
        subtitle="Manage and Deactivate this account here"
        back={true}
      />
    <div className="flex lg:flex-row flex-col gap-x-10 mt-10 mb-10 lg:px-10 px-3">
      <div className="lg:w-[60%] w-[100%]">
        <div className="prod_view_subtitle">Contact Details</div>

        <div className="mt-[0.5rem] border border-[#f1f1f1] rounded-[3px] p-3 ">
          <div className="grid grid-cols-2 prod_view_table gap-y-[1rem] prod_view_table_css">
             
            <span>Admin Mobile</span>
            <span>+{addSpaces(singleAcc?.accAdminMobile)}</span>
         
            <span>Email</span>
            <span>{singleAcc?.accAdminEmail ?? null}</span>

            <span>Office Tel</span>
            <span>{singleAcc?.accOfficeMobile ?? null}</span>
 
          </div>
        </div>
 
        <div className="prod_view_subtitle mt-10">Company Details</div>
        <div className="mt-[0.5rem] border border-[#f1f1f1] rounded-[3px] p-3 ">
          <div className="grid grid-cols-2 prod_view_table gap-y-[1rem] prod_view_table_css">
          <span>Name</span>
            <span>{singleAcc?.accName ?? "Kenya"}</span>

            <span>Website</span>
            <span>{singleAcc?.accWebsite ?? null}</span>
            

            <span>Industry</span>
            <span>{singleAcc?.accIndustry ?? null}</span>

            <span>Physical Address</span>
            <span>{singleAcc?.accPhysicalAddress ?? null}</span>

            <span>Country</span>
            <span>{singleAcc?.accCountry ?? "Kenya"}</span>

            <span>Postal Address</span>
            <span>{singleAcc?.accPostalAddress}</span>

            <span>Tax Code</span>
            <span>{singleAcc?.accPostalCode ?? null}</span>

          
          </div>
        </div>

        <div className="prod_view_subtitle mt-10">Attachments</div>

        <div className="mt-[0.5rem] border border-[#f1f1f1] rounded-[3px] p-3 ">
          <div className="grid grid-cols-2 prod_view_table gap-y-[1rem] prod_view_table_css">
          <span>Certificate of Incorporation</span>
            <span className="flex items-center text-darkGreen cursor-pointer"
                onClick={()=>handleClick(singleAcc?.accIncorporationCertFileName)}>
              <img src={svg28} alt="svg28" />
              {singleAcc?.accIncorporationCertFileName ? formatPath(singleAcc?.accIncorporationCertFileName) : "No File"}
            </span>

            <span>Tax Certificate</span>
            <span className="flex items-center text-darkGreen cursor-pointer"
                onClick={()=>handleClick(singleAcc?.accKraFileName)}>
              <img src={svg28} alt="svg28" />
              {singleAcc?.accKraFileName ? formatPath(singleAcc?.accKraFileName) : "No File"}
            </span> 

            <span>Permission Letter</span>
            <span className="flex items-center text-darkGreen cursor-pointer"
                onClick={()=>handleClick(singleAcc?.accAuthorizationFileName)}>
              <img src={svg28} alt="svg28" />
              {singleAcc?.accAuthorizationFileNam ? formatPath(singleAcc?.accAuthorizationFileName) : "No File"}
            </span>

            
          </div>
        </div>
      </div>

      <div className="lg:w-[40%] w-[100%] flex flex-col">
        <div className="prod_view_subtitle">Details</div>

       

        <div className="prod_view_table !text-[16px] mt-[1rem] mb-2">Status</div>
        <div
          className={`rounded-[1rem] p-[.25rem] ${
            singleAcc?.accStatus == "ACTIVE"
              ? "bg-[#388E3C]"
              : "bg-[#808080]"
          }  flex items-center justify-center !text-white
           w-[250px] gap-x-[5px] prod_view_table !text-[16px]`}
        >
          <img src={svg29} alt="29" />
          {singleAcc?.accStatus}
        </div>

        <div className="prod_view_table !text-[16px] mt-[1.5rem]">Actions</div>
          <div className="flex items-center mt-5 gap-x-10">
            <div className="w-[150px]">
              <button className="cstm-btn" onClick={showModalConfirmation}>Activate</button>
            </div>
            <div className="w-[150px]">
              <button className="cstm-btn !bg-red" onClick={showModalDelete}>Deactivate</button>
            </div>
          </div>
        
      </div>

      <ConfirmSubmitModal
        isModalOpen={isModalOpenConfirmation}
        setIsModalOpen={setIsModalOpenConfirmation}
        content="Are you sure?"
        subContent={`Are you sure that you want to enable ${singleAcc?.accName}`}
        handleSubmit={handleSubmit}
        loading={saving}
      />
       <DisableModal
        isModalOpen={isModalOpenDelete}
        setIsModalOpen={setIsModalOpenDelete}
        prodd={singleAcc}  
        title={`Disable ${singleAcc?.accName}`}
      />
    </div>
    </div>
  );
}

export default AccountAdminView;
