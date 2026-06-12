import { Checkbox, Form, Input, Select, Spin, message } from "antd";
import React, { useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { IdTypeData } from "../../data";
import Dragger from "antd/es/upload/Dragger";
import authorizationTemplate from "../../assets/temp/Sender ID authorisation sample letter.docx";
import ConfirmationModal from "../../components/ConfirmationModal";
import { save, saveFile } from "../../features/save/saveSlice";
import toast from "react-hot-toast";
import { CloudUploadOutlined } from "@ant-design/icons";
import { cleanRequestType } from "../../features/sms-request/smsRequestSlice";
import uplooadSimple from "../../assets/svg/UploadSimple.svg";
import { formatImgPath, formatPath } from "../../utils";
import { fetchNewProductRequest } from "../../features/product-request/productRequestSlice";

const url = process.env.REACT_APP_API_BASE_URL;
const { TextArea } = Input;
function ProductInfo({ next, prev }) {
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const linkRef = useRef(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const { saving } = useSelector((state) => state.save);

  const [isModalOpenConfirmation, setIsModalOpenConfirmation] = useState(false);
  const showModalConfirmation = async () => {
    setIsModalOpenConfirmation(true);
  };

  const [data, setData] = useState({});
  const plainOptions = ["Safaricom", "Airtel", "Telkom", "Equitel"]; 
  const [fileName, setfileName] = useState("");
  const [checkValues, setCheckValues] = useState("");
  const [loadingFile, setLoadingFile] = useState(false);
 

  function handleSelectChange(value, formName) {
    setData((prevData) => ({
      ...prevData,
      [formName]: value,
    }));
  }

  const onChangeCheckbox = (checkedValues) => {
    setCheckValues(checkedValues);
  };

  const handleBack = () => {
    prev();
  };
  async function clean() {
    await dispatch(cleanRequestType());
  }

  const onFinish = async (data) => { 
    if (!fileKra) {
      toast.error("Please attach KRA File");
      return;
    }
    if (!fileIncop) {
      toast.error("Please attach incorporate File");
      return;
    }
    if (!fileAuth) {
      toast.error("Please attach authorization File");
      return;
    }
    const res = await dispatch(
      save({
        url: "api/v2/req/sender-Id",
        telcos: checkValues?.join(","),
        senderIdType: data?.senderIdType,
        desc: data?.desc, 
        reKraFileName: fileKra,
        reIncorporationCertFileName: fileIncop,
        reAuthorizationFileName: fileAuth,
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      clean();
      fetchNewProductRequestData();
      await navigate("/code-request-list");
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

  const [fileAuth, setfileAuth] = useState(); 
  async function handleUploadAuth(e) {
    const formObject = new FormData();
    formObject.append("file", e.target.files[0]);
    const res = await dispatch(saveFile(formObject));
    if (res?.payload?.success) {
      setfileAuth(res?.payload?.data?.result);
    } else {
      toast.error(
        res?.payload?.messages?.error ??
          "There was an error uploading this file"
      );
    }
  }
  
  async function handleCancelAuth() {
    await setfileAuth();
  }
  async function handleOpenFIleAuth() {
    document.getElementById("fileAuth").click();
  }
 
  const [fileKra, setfileKra] = useState(); 
  async function handleUploadKra(e) {
    const formObject = new FormData();
    formObject.append("file", e.target.files[0]);
    const res = await dispatch(saveFile(formObject));
    if (res?.payload?.success) {
      setfileKra(res?.payload?.data?.result);
    } else {
      toast.error(
        res?.payload?.messages?.error ??
          "There was an error uploading this file"
      );
    }
  }

  async function handleOpenFIleKra() {
    document.getElementById("fileKra").click();
  }
  async function handleCancelKra() {
    await setfileKra();
  }

  const [fileIncop, setfileIncop] = useState(); 
  async function handleUploadIncop(e) {
    const formObject = new FormData();
    formObject.append("file", e.target.files[0]);
    const res = await dispatch(saveFile(formObject));
    if (res?.payload?.success) {
      setfileIncop(res?.payload?.data?.result);
    } else {
      toast.error(
        res?.payload?.messages?.error ??
          "There was an error uploading this file"
      );
    }
  }
  
  async function handleCancelIncop() {
    await setfileIncop();
  }
  async function handleOpenFIleIncop() {
    document.getElementById("fileIncop").click();
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
            <div className="product_info_title">SMS Sender ID Request</div>
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
              name="senderIdType"
              className="login-form-item"
              label={
                <span>
                  Sender Id Type <span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please select id type",
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
                  handleSelectChange(value, "usrGender");
                }}
                options={IdTypeData}
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
              rules={[
                {
                  required: true,
                  message: "Upload file .pdf",
                },
              ]}
              name={"fileUrl"}
              label={
                <span className="">
                  Permission Letter<span className="text-[#FF0000]">*</span>
                  <span className="text-[#2C963F]">
                    {" "}
                    <a
                      ref={linkRef}
                      className=""
                      href={authorizationTemplate}
                      download
                      onClick={(e) => e.stopPropagation()}
                    >
                      <span onClick={handleClick}>
                        Click here to download a draft authorization letter of the sender ID.
                      </span>
                    </a>
                  </span>
                  Please fill it out accordingly; then scan and upload it below
                  in PDF format.
                </span>
              }
            >
              {fileAuth ? (
                <>
                  <div className="text-blueDark w-full flex justify-between items-center h-auto px-3 input truncate">
                    <span className="text-[18px]">{formatPath(fileAuth)}</span>

                    <button
                      type="button"
                      onClick={handleCancelAuth}
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
                  <div className="input relative" onClick={handleOpenFIleAuth}>
                    <input
                      className="!hidden"
                      accept=".pdf"
                      id="fileAuth"
                      name="file"
                      onChange={(e) => handleUploadAuth(e)}
                      type="file"
                    />
                    <div className="absolute inset-0 flex items-center justify-end pointer-events-none px-3">
                      <img src={uplooadSimple} alt="uploadSimple" />
                    </div>
                  </div>
                </>
              )}
            </Form.Item>

            <div className="flex lg:flex-row flex-col gap-x-5">
              <Form.Item
                className="w-full"
                rules={[
                  {
                    required: false,
                    message: "Upload file .pdf",
                  },
                ]}
                name={"fileKra"}
                label={<span className="">KRA Cert</span>}
              >
                {fileKra ? (
                  <>
                    <div className="text-blueDark w-full flex justify-between items-center h-auto px-3 input truncate">
                      <span className="text-[18px]">{formatPath(fileKra)}</span>

                      <button
                        type="button"
                        onClick={handleCancelKra}
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
                    <div className="input relative" onClick={handleOpenFIleKra}>
                      <input
                        className="!hidden"
                        accept=".pdf"
                        id="fileKra"
                        name="file"
                        onChange={(e) => handleUploadKra(e)}
                        type="file"
                      />
                      <div className="absolute inset-0 flex items-center justify-end pointer-events-none px-3">
                        <img src={uplooadSimple} alt="uploadSimple" />
                      </div>
                    </div>
                  </>
                )}
              </Form.Item>
              <Form.Item
                className="w-full"
                rules={[
                  {
                    required: false,
                    message: "Upload file .pdf",
                  },
                ]}
                name={"fileIncop"}
                label={<span className="">Incorporation Cert</span>}
              >
                {fileIncop ? (
                  <>
                    <div className="text-blueDark w-full flex justify-between items-center h-auto px-3 input truncate">
                      <span className="text-[18px]">
                        {formatPath(fileIncop)}
                      </span>

                      <button
                        type="button"
                        onClick={handleCancelIncop}
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
                    <div className="input relative" onClick={handleOpenFIleIncop}>
                      <input
                        className="!hidden"
                        accept=".pdf"
                        id="fileIncop"
                        name="file"
                        onChange={(e) => handleUploadIncop(e)}
                        type="file"
                      />
                      <div className="absolute inset-0 flex items-center justify-end pointer-events-none px-3">
                        <img src={uplooadSimple} alt="uploadSimple" />
                      </div>
                    </div>
                  </>
                )}
              </Form.Item>
 
            </div>

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
                <button
                  type="submit"
                  key="submit"
                  className={`cstm-btn`}
                  disabled={saving}
                >
                  {saving ? <Spin /> : "Finish"}
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

export default ProductInfo;
