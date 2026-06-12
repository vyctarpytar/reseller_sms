import { useEffect, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { Form, Input, Spin } from "antd";
import {
  downloadMembersExcel,
  fetchContactFolders,
} from "../../../features/contact/ContactSlice";
import graduateTemplate from "../../../assets/temp/GraduatesListUploadTemplat.xlsx";
import InsideHeader from "../../../components/InsideHeader";
import download from "../../../assets/svg/file_download-24px.svg";
import upload from "../../../assets/svg/file_upload-24px.svg";
import ContactExcelModal from "./modal/ContactExcelModal";
import toast from "react-hot-toast";

const url = process.env.REACT_APP_API_BASE_URL;

export default function ContactUploadList() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const linkRef = useRef(null);

  const { folderObj } = useSelector((state) => state.folder);
  const { user } = useSelector((state) => state.auth);
  const { gradLoading, contactsHeaders, gradUpload, gradListObj } = useSelector(
    (state) => state.contact
  );

  const [formData, setFormData] = useState({
    gdcInstId: user?.usrInstId,
    gdcCreatedBy: user?.usrId,
    gdcFldId: folderObj?.fldId,
  });
  const [fileUrl, setfileUrl] = useState();
  const [loading, setloading] = useState(false);

  const [downloaded, setDownloaded] = useState(false);

 
  const handleClick = async (event) => {
    event.preventDefault(); 
    const res = await dispatch(downloadMembersExcel());

    if (res?.payload) { 
      const blob = new Blob([res.payload], {
        type: "application/octet-stream",
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", "template.xlsx");  
      document.body.appendChild(link);
      link.click();

 
      link.parentNode.removeChild(link);

      toast.success("File downloaded successfully");
    } else {
      toast.error("Failed to download file");
    }
  };
  const [openExcelUpload, setopenExcelUpload] = useState(false);
  function handleCancelExcelUpload() {
    setopenExcelUpload(false);
  }

  async function handleCancel() {
    await setfileUrl();
  }

  const handleSuccess = (response) => {
    setfileUrl(response);
  };

  const handleError = (response) => {
    // Handle error logic
    console.error("File upload failed:", response);
  };

  function handleGoBack() {
    navigate(-1);
  }

  async function handleFetchData() {
    await dispatch(fetchContactFolders(folderObj?.groupId));
  }

  function handleBackToList() {
    navigate(`/contacts/folder/${folderObj?.fldName}`);
  }

 

  const onFinish = async (values) => {
    setloading(true);
    values.gdcInstId = user?.usrInstId;
    values.gdcCreatedBy = user?.usrId;
    values.gdcFldId = folderObj?.fldId;

    let formData = new FormData();
    let success;

    formData.append("file", fileUrl);

    if (!folderObj?.fldId) {
      toast.error("A folder must be selected before uploading a list");
      await navigate(`/contacts`);
    }

    if (!values.gdcFileUrl) {
      setloading(false);
      toast.error("Please select a file before submitting");
    }

    await fetch(
      `${url}/emp/uploadGraduateExcel.action?gdcInstId=${
        user?.usrInstId
      }&gdcCreatedBy=
			${user?.usrId}&gdcFldId=${folderObj?.fldId}&gdcTitle=${values.gdcTitle}&gdcId=${
        gradUpload ? gradListObj?.gdcId : null
      }`,
      {
        method: "POST",
        body: formData,
      }
    )
      .then((res) => res.json())
      .then((data) => {
        success = data.success;
        if (data?.success) {
          toast.success("List of contacts uploaded");
          form.resetFields();
          setloading(false);
        } else {
          toast.error(data?.messages?.message);
          setloading(false);
        }
      });

    if (success) {
      await handleFetchData();
      await handleBackToList();
    }
  };

  useEffect(() => { 
  }, [contactsHeaders, gradUpload, gradListObj]);

  return (
    <div className="w-full overflow-y-scroll">
      <InsideHeader
        title="Contacts"
        subtitle="Manage contacts within your institution"
        back={true}
      />

      <div className="px-10 mt-[51px]">
        <span className="text-black1  text-[24px] font-bold dash-title leading-[32.4px]">
          Import list of contacts (excel file)
        </span>

        <div
          className="mt-[31px] bg-white flex flex-col justify-center items-center border bottom-solid border-[#F5F7FF]
		w-[912px] h-[306px] px-0 py-[60px]"
        >
          {downloaded ? (
            <>
              <div className="text-black1 text-[18px] font-normal font-dmSans leading-[24px]">
                You have downloaded the template
              </div>
              <div className="mt-[10px] text-[#616161] text-[16px] font-normal leading-[24px]">
                Fill in the template with the list of contacts and upload it
                when you are done
              </div>
              <div className="flex flex-col mt-[20px]">
                <a
                  className="cursor-pointer w-[250px] h-[50px] px-[24px] py-3 bg-blueDark rounded-[28px] justify-center items-center gap-1 inline-flex
				text-white text-[16px] font-medium "
                  download
                  onClick={() => setopenExcelUpload(true)}
                >
                  <img src={upload} alt="upload-icon" />
                  Upload template
                </a>
                <a
                  className="mt-[10px] w-[250px] h-[50px] px-[24px] py-3 border-none text-darkBlue rounded-[28px] justify-center items-center gap-1 inline-flex
				text-[16px] font-medium "
                  href={graduateTemplate}
                  download
                  onClick={() => setDownloaded(true)}
                >
                  <img src={download} alt="download-icon" />
                  Re-download template
                </a>
              </div>
            </>
          ) : (
            <>
              <div className="text-black1 text-[18px] font-normal font-dmSans leading-[24px]">
                Complete the following steps to import your list of contacts
              </div>

              <div className="mt-[10px] text-[#616161] text-[16px] font-normal leading-[24px]">
                Step 1. Download the excel template by clicking the button
                below.
                <br />
                Step 2. Fill in the template with the list of contacts.
                <br />
                Step 3. Upload the filled in template.
              </div>

              <div className="flex flex-col mt-[20px] justify-center items-center">
                <div
                  className="w-[250px] h-[50px] px-[24px] py-3 bg-darkGreen rounded-[28px] justify-center items-center gap-1 inline-flex text-white text-[16px] font-medium cursor-pointer"
                  onClick={handleClick}
                >
                  <span className="flex items-center gap-x-[4px]">
                    <img src={download} alt="download-icon" />
                    Download template
                  </span>
                </div>

                <div
                  className="mt-[10px] w-full h-[50px] px-[24px] py-3 border-none text-darkBlue rounded-[28px] justify-center items-center gap-1 inline-flex
				              text-[16px] font-medium cursor-pointer"
                  onClick={() => setopenExcelUpload(true)}
                >
                  Already have template ? Proceed to upload
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      <ContactExcelModal
        open={openExcelUpload}
        handleCancelModal={handleCancelExcelUpload}
      />
    </div>
  );
}
