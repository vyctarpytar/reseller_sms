import { Checkbox, Form, Input, Select, Spin, message } from "antd";
import React, { useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { IdTypeData, costData, ownershipData } from "../../data";
import Dragger from "antd/es/upload/Dragger";
import employeeTemplate from "../../assets/temp/Employee Returns Template.xlsx";
import ConfirmationModal from "../../components/ConfirmationModal";
import { save, saveFile } from "../../features/save/saveSlice";
import toast from "react-hot-toast";
import { cleanRequestType } from "../../features/sms-request/smsRequestSlice";
import { CloudUploadOutlined } from "@ant-design/icons";
import uplooadSimple from "../../assets/svg/UploadSimple.svg";
import { fetchNewProductRequest } from "../../features/product-request/productRequestSlice";


const url = process.env.REACT_APP_API_BASE_URL;
const { TextArea } = Input;
function UssdInfo({ next, prev }) {
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const linkRef = useRef(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const {saving} = useSelector((state)=>state.save)


  const [isModalOpenConfirmation, setIsModalOpenConfirmation] = useState(false);
  const showModalConfirmation = async () => {
    setIsModalOpenConfirmation(true);
  };

  
  const [data, setData] = useState({});
  const plainOptions = ["Safaricom", "Airtel", "Telkom", "Equitel"];
  const [fileUrl, setfileUrl] = useState("");
  const [fileName, setfileName] = useState("");
  const [checkValues,setCheckValues] = useState("")
  const [loadingFile, setLoadingFile] = useState(false);


  async function handleFileUpload(e) {
    setLoadingFile(true);
    let file = e.target.files[0];
    const formObject = new FormData();
    formObject.append("file", file);
    const res = await dispatch(saveFile(formObject)); 
    if (res?.payload?.success) {
      setfileUrl(res?.payload?.data?.result);
    } else {
      message.error(
        res?.payload?.messages?.error ??
          "There was an error uploading this file"
      );
    }
    setLoadingFile(false);
  }

  

  async function handleRemoveFile(file) {
    setfileUrl("");
  }

  function handleSelectChange(value, formName) {
    setData((prevData) => ({
      ...prevData,
      [formName]: value,
    })); 
  }

  const onChangeCheckbox = (checkedValues) => {
    setCheckValues(checkedValues) 
  };

  const handleBack = () => {
    prev();
  };
  async function clean(){
    await dispatch(cleanRequestType())
    }
  
  const onFinish = async(data) => {
    if(!fileUrl){
      toast.error("Please select file");
      return
    }
    const res = await dispatch(save({
      url:'api/v2/req/ussd-code',
      telcos: checkValues?.join(","), 
      costCover:data?.costCover,
      desc:data?.desc,
      file:fileUrl
    }))
    if (res?.payload?.success) {
      toast.success(res?.payload?.messages?.message);
      clean();
      fetchNewProductRequestData()
      await navigate('/code-request-list')
    } else {
      toast.error(res?.payload?.messages?.message);
    } 
  };

  async function fetchNewProductRequestData() {
    const res = await dispatch(
      fetchNewProductRequest({
        reqStatus: "PENDING",
      })
    );
  }
  const handleClick = (event) => {
    event.preventDefault();
    if (linkRef.current) {
      linkRef.current.click(); 
    }
  };


  async function handleUpload(e) {
    const formObject = new FormData();
    formObject.append("file", e.target.files[0]);
    const res = await dispatch(saveFile(formObject)); 
    if (res?.payload?.success) {
      setfileUrl(res?.payload?.data?.result);
    } else {
      toast.error(
        res?.payload?.messages?.error ??
          "There was an error uploading this file"
      );
    }
    // setfileUrl(e.target.files[0]);
  }

  async function handleOpenFIle() {
    document.getElementById("file").click();
  }
  async function handleCancelFile() {
    await setfileUrl();
  }

  return (
    <>
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          marginBottom: "2.5rem",
          marginTop: "2.5rem",
        }}
      >
        <Form
          layout="vertical"
          ref={formRef}
          name="control-ref"
          onFinish={onFinish}
          style={{
            maxWidth: "100%",
          }}
          className="w-[850px]"
          form={form}
        >
          <div className="grid grid-cols-1 lg:px-10 px-3 py-10 border border-[#D4D4D8]">
            <div className="product_info_title">Ussd Service Code Request
            </div>
            <Form.Item
              name="telcos"
              label={
                <span>
                  Telcos <span className="text-[#FF0000]">*</span>
                </span>
              }
              className="login-form-item "
              rules={[
                {
                  required: true,
                  message: "Please delect telcos",
                },
              ]}
            >
              <div
                className="w-full lg:h-[100px] h-auto border border-[#D4D4D8] rounded-[6px] flex 
              lg:flex-row flex-col lg:items-center justify-start lg:p-10 p-3"
              >
                <Checkbox.Group
                  className="flex lg:flex-row flex-col gap-y-2"
                  options={plainOptions}
                  onChange={onChangeCheckbox}
                />
              </div>
            </Form.Item>
  

            <Form.Item 
             name="costCover"
              className=""
              label={
                <span>
                Who do you want to cover the cost of each USSD session? <span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please select cost liable",
                },
              ]}
            >
              <Select 
                className=""
                allowClear
                style={{
                  width: "100%",
                }}
                onChange={(value) => {
                  handleSelectChange(value, "costCover");
                }} 
                options={
                  costData?.length > 0 &&
                  costData?.map((item) => ({
                    value: item?.value,
                    label: item?.label,
                  }))
                }
                showSearch
                optionFilterProp="children"
                filterOption={(input, option) =>
                  (option?.label?.toLocaleLowerCase() ?? "").includes(
                    input?.toLocaleLowerCase()
                  )
                }
                onDropdownVisibleChange={() => {}}
              />
            </Form.Item>
 
            <Form.Item
              name="desc"
              label={
                <span>
                  Description <span className="text-[#FF0000]">*</span>
                  Please describe in detail how you intend to use the service(s)
                  requested
                </span>
              }
              rules={[
                {
                  required: true,
                  message:
                    "Please describe in detail how you intend to use the service(s) requested",
                },
              ]}
            >
              <TextArea
                placeholder="Please describe in detail how you intend to use the service(s) requested"
                rows={4}
                className="input-textarea"
              />
            </Form.Item>


            <Form.Item
              extra={"Upload file .pdf"}
              rules={[
                {
                  required: true,
                  message: "Upload file .pdf",
                },
              ]}
              name={"gdcFileUrl"}
              label={
                <span className="">
                  Signed Letter<span className="text-[#FF0000]">*</span>
                  <span className="text-[#2C963F]">
                    {" "}
                    <a
                      ref={linkRef}
                      className=""
                      href={employeeTemplate}
                      download
                      onClick={(e) => e.stopPropagation()}
                    >
                      <span onClick={handleClick}>
                        Click here to download a draft letter of the sender ID.
                      </span>
                    </a>
                  </span>
                  Please fill it out accordingly; then scan and upload it below
                  in PDF format.
                </span>
              }
            >
              {fileUrl?.name ? (
                <>
                  <div className="text-blueDark w-full flex justify-between items-center h-auto px-3 input truncate">
                    <span className="text-[18px]">{fileUrl?.name}</span>

                    <button
                      type="button"
                      onClick={handleCancelFile}
                      className="flex items-center"
                    >
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        width="25"
                        height="24"
                        viewBox="0 0 25 24"
                        fill="none"
                      >
                        <path
                          d="M9.5 13H15.5C15.7652 13 16.0196 12.8946 16.2071 12.7071C16.3946 12.5196 16.5 12.2652 16.5 12C16.5 11.7348 16.3946 11.4804 16.2071 11.2929C16.0196 11.1054 15.7652 11 15.5 11H9.5C9.23478 11 8.98043 11.1054 8.79289 11.2929C8.60536 11.4804 8.5 11.7348 8.5 12C8.5 12.2652 8.60536 12.5196 8.79289 12.7071C8.98043 12.8946 9.23478 13 9.5 13ZM21.5 2H3.5C3.23478 2 2.98043 2.10536 2.79289 2.29289C2.60536 2.48043 2.5 2.73478 2.5 3V21C2.5 21.2652 2.60536 21.5196 2.79289 21.7071C2.98043 21.8946 3.23478 22 3.5 22H21.5C21.7652 22 22.0196 21.8946 22.2071 21.7071C22.3946 21.5196 22.5 21.2652 22.5 21V3C22.5 2.73478 22.3946 2.48043 22.2071 2.29289C22.0196 2.10536 21.7652 2 21.5 2ZM20.5 20H4.5V4H20.5V20Z"
                          fill="#147CBC"
                        />
                      </svg>
                    </button>
                  </div>
                </>
              ) : (
                <>
                  <div className="input relative" onClick={handleOpenFIle}>
                    <input
                      className="!hidden"
                      accept=".pdf"
                      id="file"
                      name="file"
                      onChange={(e) => handleUpload(e)}
                      type="file"
                    />
                    <div className="absolute inset-0 flex items-center justify-end pointer-events-none px-3">
                      <img src={uplooadSimple} alt="uploadSimple" />
                    </div>
                  </div>
                </>
              )}
            </Form.Item>
            {/* <div className="flex flex-col">
                <span className="">
                  Signed Letter<span className="text-[#FF0000]">*</span>
                  <span className="text-[#2C963F]">
                    {" "}
                    <a
                      ref={linkRef}
                      className=""
                      href={employeeTemplate}
                      download
                      onClick={(e) => e.stopPropagation()}
                    >
                      <span onClick={handleClick}>
                        Click here to download a draft letter of the sender ID.
                      </span>
                    </a>
                  </span>
                  Please fill it out accordingly; then scan and upload it below
                  in PDF format.
                </span>  
              <div className="mt-[.25rem]">
                <label className="flex flex-row items-center justify-start w-[100%]">
                  <input
                    name="file"
                    type="file"
                    accept=".pdf"
                    style={{ display: "none" }}
                    onChange={handleFileUpload}
                  />
                  <div className="input flex items-center justify-center sms-spin">
                    {loadingFile ? (
                      <Spin />
                    ) : (
                      <>
                        {" "}
                        <svg
                          xmlns="http://www.w3.org/2000/svg"
                          width="29"
                          height="29"
                          viewBox="0 0 29 29"
                          fill="none"
                          className="mt-[0px]"
                        >
                          <path
                            d="M25.375 18.125V22.9583C25.375 23.5993 25.1204 24.214 24.6672 24.6672C24.214 25.1204 23.5993 25.375 22.9583 25.375H6.04167C5.40073 25.375 4.78604 25.1204 4.33283 24.6672C3.87961 24.214 3.625 23.5993 3.625 22.9583V18.125"
                            stroke="#147CBC"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          />
                          <path
                            d="M20.5423 9.66667L14.5007 3.625L8.45898 9.66667"
                            stroke="#147CBC"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          />
                          <path
                            d="M14.5 3.625V18.125"
                            stroke="#147CBC"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          />
                        </svg>
                        <p className="text-[16px] !ml-2">Upload File</p>
                      </>
                    )}
                  </div>
                </label>
              </div>

              <div className="flex flex-col w-full truncate mt-[1rem]">
                {fileUrl && (
                  <div className="flex justify-between items-center">
                    <div className="flex items-center mt-[.5rem]">
                      <CloudUploadOutlined className="text-2xl text-[#388E3C]" />
                      <span className="label_1 ml-[.5rem] w-full truncate">
                        {fileUrl?.split("_").pop()}
                      </span>
                    </div>
                    <button onClick={() => handleRemoveFile()}>
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        width="20"
                        height="20"
                        viewBox="0 0 24 24"
                        fill="none"
                      >
                        <path
                          d="M13.4099 12.0002L19.7099 5.71019C19.8982 5.52188 20.004 5.26649 20.004 5.00019C20.004 4.73388 19.8982 4.47849 19.7099 4.29019C19.5216 4.10188 19.2662 3.99609 18.9999 3.99609C18.7336 3.99609 18.4782 4.10188 18.2899 4.29019L11.9999 10.5902L5.70994 4.29019C5.52164 4.10188 5.26624 3.99609 4.99994 3.99609C4.73364 3.99609 4.47824 4.10188 4.28994 4.29019C4.10164 4.47849 3.99585 4.73388 3.99585 5.00019C3.99585 5.26649 4.10164 5.52188 4.28994 5.71019L10.5899 12.0002L4.28994 18.2902C4.19621 18.3831 4.12182 18.4937 4.07105 18.6156C4.02028 18.7375 3.99414 18.8682 3.99414 19.0002C3.99414 19.1322 4.02028 19.2629 4.07105 19.3848C4.12182 19.5066 4.19621 19.6172 4.28994 19.7102C4.3829 19.8039 4.4935 19.8783 4.61536 19.9291C4.73722 19.9798 4.86793 20.006 4.99994 20.006C5.13195 20.006 5.26266 19.9798 5.38452 19.9291C5.50638 19.8783 5.61698 19.8039 5.70994 19.7102L11.9999 13.4102L18.2899 19.7102C18.3829 19.8039 18.4935 19.8783 18.6154 19.9291C18.7372 19.9798 18.8679 20.006 18.9999 20.006C19.132 20.006 19.2627 19.9798 19.3845 19.9291C19.5064 19.8783 19.617 19.8039 19.7099 19.7102C19.8037 19.6172 19.8781 19.5066 19.9288 19.3848C19.9796 19.2629 20.0057 19.1322 20.0057 19.0002C20.0057 18.8682 19.9796 18.7375 19.9288 18.6156C19.8781 18.4937 19.8037 18.3831 19.7099 18.2902L13.4099 12.0002Z"
                          fill="#212121"
                        />
                      </svg>
                    </button>
                  </div>
                )}
              </div>
            </div> */}

            <div className="mt-[20px]  lg:mb-0 mb-[20px] flex lg:px-0 px-3   justify-end  gap-x-4">
              <div className="w-[69px]">
                <button
                type="button"
                  className={`cstm-btn !bg-[#fff] !text-[#374151] border border-[#D1D5DB]`}
                  onClick={showModalConfirmation}
                >
                  Back
                </button>
              </div>

              <div className="w-[109px]">
                <button type="submit" key="submit" className={`cstm-btn`}
                disabled={saving}>
                {saving ? <Spin/> :'Finish'}  
                </button>
              </div>
            </div>
          </div>
        </Form>
      </div>

      <ConfirmationModal
        isModalOpen={isModalOpenConfirmation}
        setIsModalOpen={setIsModalOpenConfirmation}
        handleBack={handleBack} 
        content="Are you sure?"
        subContent="Are you sure that you want to leave this page? You will lose any unsaved changes."
      />
    </>
  );
}

export default UssdInfo;
