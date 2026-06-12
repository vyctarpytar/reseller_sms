import React, { useEffect, useRef, useState } from "react";
import { Button, Card, DatePicker, Form, Input, Modal, Spin } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import MaterialIcon from "material-icons-react";
import uplodSvg from "../../assets/img/uploadPic.png";
import toast from "react-hot-toast";
import {
  formatImgPath,
  formatPath,
  normalizeDateToLocalYearOnly,
  removeCommas,
  removeNegative,
} from "../../utils";
import dayjs from "dayjs";
import { save, saveFile } from "../../features/save/saveSlice";
import PhoneInput from "react-phone-input-2";
import { fetchReseller } from "../../features/reseller/resellerSlice";
import { fetchResellerAccounts } from "../../features/reseller-account/resellerAccountSlice";
import uplooadSimple from "../../assets/svg/UploadSimple.svg";

const { TextArea } = Input;
const AccountAddModal = ({ isModalOpen, setIsModalOpen, prodd }) => {
  const handleOk = () => {
    setIsModalOpen(false);
  };
  const handleCancel = () => {
    setIsModalOpen(false);
  };
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const { user } = useSelector((state) => state.auth);
  const { saving } = useSelector((state) => state.save);

  const linkRef = useRef(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  function closeModal() {
    toast.dismiss();
  }

  const [formData, setFormData] = useState({ ...prodd });

  function handleSelectChange(value, formName) {
    setFormData((prevData) => ({
      ...prevData,
      [formName]: value,
    }));
  }

  const [data, setdata] = useState({});

  const handleNumberChangeAdminTel = (e) => {
    setFormData((prevData) => ({
      ...prevData,
      accAdminMobile: String(e),
    }));
  };

useEffect(() => {
  if (prodd) {
    setFormData({
      ...prodd,
      accLicenceValidity: prodd?.accLicenceValidity
      ? dayjs(prodd?.accLicenceValidity.toString(), "YYYY")  
      : null,
    });
  }
}, [prodd]);


  const onChange = async (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };
    function onMonthChange(value) {
    setFormData((prevData) => ({
      ...prevData,
      accLicenceValidity: value,
    }));
  }


  function fetchResellerAccountData() {
    dispatch(fetchResellerAccounts());
  }
  
  const onFinish = async (data) => {
    // if (!formData?.accAssignedTo) {
    //   toast.error("Please select user to assign");
    //   return;
    // }
    const res = await dispatch(
      save({
        url: "api/v2/account",
        accId: prodd?.accId ? prodd?.accId : null,
        accName: formData?.accName,
        accWebsite: formData?.accWebsite,
        accAdminMobile: formData?.accAdminMobile,
        accAdminEmail: formData?.accAdminEmail,
        accOfficeTel: formData?.accOfficeTel,
        accPostalAddress: formData?.accPostalAddress,
        accPostalCode: formData?.accPostalCode,
        accCity: formData?.accCity,
        accIndustry: formData?.accIndustry,
        // accAssignedTo: formData?.accAssignedTo,
        accCode: formData?.accCode,
        accPhysicalAddress: formData?.accPhysicalAddress,
        accSmsPrice: removeNegative(formData?.accSmsPrice),
        accDeliveryUrl: formData?.accDeliveryUrl,
        accKraFileName: fileKra,
        accIncorporationCertFileName: fileIncop,
        accAuthorizationFileName: fileAuth,
        accLicenceValidity:normalizeDateToLocalYearOnly(formData?.accLicenceValidity),
        accMsgBal:formData?.accMsgBal
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await fetchResellerAccountData();
      await form.resetFields();
      await setIsModalOpen(false);
      await navigate("/sender-id-list");
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  const handleClick = (event) => {
    event.preventDefault();
    if (linkRef.current) {
      linkRef.current.click();
    }
  };
  const [fileKra, setfileKra] = useState(prodd?.accKraFileName);
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

  async function handleOpenFIle() {
    document.getElementById("file").click();
  }
  async function handleCancelKra() {
    await setfileKra();
  }

  const [fileIncop, setfileIncop] = useState(
    prodd?.accIncorporationCertFileName
  );
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
  async function handleOpenFIle() {
    document.getElementById("file").click();
  }
  async function handleCancelIncop() {
    await setfileIncop();
  }

  const [fileAuth, setfileAuth] = useState(prodd?.accAuthorizationFileName);
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
  async function handleOpenFIle() {
    document.getElementById("file").click();
  }
  async function handleCancelAuth() {
    await setfileAuth();
  }
  console.log("prodd",prodd)
  return (
    <>
      <Modal
        className=""
        title={`${
          prodd?.accId ? `Update Account - ${prodd.accId}` : "New Account"
        }`}
        open={isModalOpen}
        onOk={onFinish}
        onCancel={handleCancel}
        width={900}
      >
        <Form
          layout="vertical"
          ref={formRef}
          name="control-ref"
          onFinish={onFinish}
          className="px-[15px]"
          style={{
            maxWidth: "100%",
          }}
          form={form}
        >
          <div className="grid lg:grid-cols-2 grid-cols-1 lg:gap-x-5 lg:gap-y-0 gap-y-5">
            <Form.Item
              label={
                <span>
                  Account Name<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add sms unit price",
                },
              ]}
            >
              <Input
                required
                name="accName"
                onChange={onChange}
                value={formData?.accName}
                className="input"
              />
            </Form.Item>
            <Form.Item
              label={
                <span>
                  Admin Mobile<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add office mobile no",
                },
              ]}
            >
              <PhoneInput
                required
                name="accAdminMobile"
                onChange={handleNumberChangeAdminTel}
                value={formData?.accAdminMobile}
                country="ke"
                onlyCountries={["ke"]}
                countryCodeEditable={false}
                className="input rounded-[6px] border !border-[#cacaca] !h-[42px]"
              />
            </Form.Item>
            <Form.Item
              label={
                <span>
                  Admin Email<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add admin email",
                },
                {
                  type: "email",
                  message: "Please enter a valid email address",
                },
              ]}
            >
              <Input
                type="email"
                required
                name="accAdminEmail"
                onChange={onChange}
                value={formData?.accAdminEmail}
                className="input"
              />
            </Form.Item>

            <Form.Item
              label={
                <span>
                  Sms Price<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add sms price",
                },
              ]}
            >
              <Input
                required
                type="number"
                name="accSmsPrice"
                onChange={onChange}
                value={formData?.accSmsPrice}
                className="input"
              />
            </Form.Item>
            {
              prodd?.accId && (
 <Form.Item
              label={
                <span>
                  Credit Balance<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add Credit Balance",
                },
              ]}
            >
              <Input
                required
                type="number"
                name="accMsgBal"
                onChange={onChange}
                value={formData?.accMsgBal}
                className="input"
              />
            </Form.Item>
              )
            }
            
            

            <Form.Item
              label="Website"
              rules={[
                {
                  required: false,
                  message: "Please add website",
                },
                {
                  type: "url",
                  message: "Please add a valid website URL",
                },
              ]}
            >
              <Input
                name="accWebsite"
                value={formData?.accWebsite}
                onChange={onChange}
                className="input"
              />
            </Form.Item>

            <Form.Item
              label={<span>Office tel </span>}
              rules={[
                {
                  required: false,
                  message: "Please add office tel",
                },
              ]}
            >
              <Input
                name="accOfficeTel"
                onChange={onChange}
                value={formData?.accOfficeTel}
                className="input"
              />
            </Form.Item>

            <Form.Item
              label={<span>Postal Address</span>}
              rules={[
                {
                  required: false,
                  message: "Please add postal address",
                },
              ]}
            >
              <Input
                name="accPostalAddress"
                onChange={onChange}
                value={formData?.accPostalAddress}
                className="input"
              />
            </Form.Item>

            <Form.Item
              label={<span>Postal Code</span>}
              rules={[
                {
                  required: false,
                  message: "Please add postal code",
                },
              ]}
            >
              <Input
                name="accPostalCode"
                onChange={onChange}
                value={formData?.accPostalCode}
                className="input"
              />
            </Form.Item>

            <Form.Item
              label={<span>City</span>}
              rules={[
                {
                  required: false,
                  message: "Please add city",
                },
              ]}
            >
              <Input
                name="accCity"
                onChange={onChange}
                value={formData?.accCity}
                className="input"
              />
            </Form.Item>

            <Form.Item
              label={<span>Industry</span>}
              rules={[
                {
                  required: false,
                  message: "Please add industry",
                },
              ]}
            >
              <Input
                name="accIndustry"
                onChange={onChange}
                value={formData?.accIndustry}
                className="input"
              />
            </Form.Item>

            {/* <Form.Item
              className=""
              label={
                <span>
                  Assigned To<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please select business type",
                },
              ]}
            >
              <Select
                name="accAssignedTo"
                className=""
                allowClear
                style={{
                  width: "100%",
                }}
                onChange={(value) => {
                  handleSelectChange(value, "accAssignedTo");
                }}
                value={formData?.accAssignedTo}
                options={
                  userData?.length > 0 &&
                  userData?.map((item) => ({
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
            </Form.Item> */}
            <Form.Item
              label={<span>Code</span>}
              rules={[
                {
                  required: false,
                  message: "Please add code",
                },
              ]}
            >
              <Input
                name="accCode"
                onChange={onChange}
                value={formData?.accCode}
                className="input"
              />
            </Form.Item>

            <Form.Item
              label={<span>Physical Address</span>}
              rules={[
                {
                  required: false,
                  message: "Please add physical address",
                },
              ]}
            >
              <Input
                name="accPhysicalAddress"
                onChange={onChange}
                value={formData?.accPhysicalAddress}
                className="input"
              />
            </Form.Item>

            <Form.Item label="License Validity" className="mr-6 w-full">
              <DatePicker
                name="accLicenceValidity"
                style={{
                  width: "100%",
                  height: "42px",
                }}
                className="mr-3"
                picker="year"
                format={"YYYY"}
                value={formData?.accLicenceValidity}
                onChange={onMonthChange}
              />
            </Form.Item>

            <Form.Item
              label={<span>Delivery URL</span>}
              rules={[
                {
                  required: false,
                  message: "Please add sms delivery URL",
                },
              ]}
            >
              <Input
                name="accDeliveryUrl"
                onChange={onChange}
                value={formData?.accDeliveryUrl}
                className="input"
              />
            </Form.Item>

            <Form.Item
              extra={"Upload file .pdf"}
              rules={[
                {
                  required: false,
                  message: "Upload file .pdf",
                },
              ]}
              name={"gdcFileUrl"}
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
                  <div className="input relative" onClick={handleOpenFIle}>
                    <input
                      className="!hidden"
                      accept=".pdf"
                      id="file"
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
              extra={"Upload file .pdf"}
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
                    <span className="text-[18px]">{formatPath(fileIncop)}</span>

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
                  <div className="input relative" onClick={handleOpenFIle}>
                    <input
                      className="!hidden"
                      accept=".pdf"
                      id="file"
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

            <Form.Item
              extra={"Upload file .pdf"}
              rules={[
                {
                  required: false,
                  message: "Upload file .pdf",
                },
              ]}
              name={"fileAuth"}
              label={<span className="">Authorization Cert</span>}
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
                  <div className="input relative" onClick={handleOpenFIle}>
                    <input
                      className="!hidden"
                      accept=".pdf"
                      id="file"
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
          </div>

          <div className="flex justify-between mt-[48px] ">
            <div className="justify-start"></div>
            <div className="justify-end flex items-center mb-[58px] gap-x-5">
              <div className="w-[150px] ">
                <button
                  key="back"
                  type="button"
                  onClick={handleCancel}
                  className="cstm-btn !bg-white !text-[var(--brand)] !border !border-[var(--brand)]"
                >
                  Cancel
                </button>
              </div>

              <div className="w-[150px]">
                <button
                  key="submit"
                  type="submit"
                  className="cstm-btn"
                  disabled={saving}
                >
                  {saving ? <Spin /> : "Create"}
                </button>
              </div>
            </div>
          </div>
        </Form>
      </Modal>
    </>
  );
};
export default AccountAddModal;
