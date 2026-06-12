import React, { useEffect, useRef, useState } from "react";
import { Form, Input, Modal, Spin, Select, InputNumber } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import MaterialIcon from "material-icons-react";
import uplodSvg from "../../assets/img/uploadPic.png";
import toast from "react-hot-toast";
import { formatImgPath } from "../../utils";
import { instTypeData } from "../../data";
import { save, saveFile } from "../../features/save/saveSlice";
import PhoneInput from "react-phone-input-2";
import {
  fetchReseller,
  fetchResellerImage,
} from "../../features/reseller/resellerSlice";

const url = process.env.REACT_APP_API_BASE_URL;

const ResellerAddModal = ({
  isModalOpen,
  setIsModalOpen,
  prodd,
  previewUrl,
  setPreviewUrl,
  preview,
  setPreview,
}) => {
  const handleOk = () => {
    setIsModalOpen(false);
  };
  const [profile, setProfile] = useState("");
  
  const handleCancel =async () => {
    await setIsModalOpen(false);
    await setPreviewUrl('')
    await setProfile("")
  };
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const { user } = useSelector((state) => state.auth);
  const { saving } = useSelector((state) => state.save);
  const { resellerImage, loading } = useSelector((state) => state.reseller);

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

  const handleNumberChange = (e) => {
    setFormData((prevData) => ({
      ...prevData,
      rsPhoneNumber: String(e),
    }));
  };

  useEffect(() => {
    setFormData(prodd);
  }, [prodd]);

  const onChange = async (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  const onChangeOther = (value, fieldName) => {
    setFormData((prev) => ({
      ...prev,
      [fieldName]: value,
    }));
  };

  function fetchResellerData() {
    dispatch(fetchReseller());
  }

  const [loadingAttach, setLoadingAttach] = useState(false);
 
 
  const handleImageSelect = async (event) => {
    setLoadingAttach(true);
    const file = event.target.files[0];
    const formData = new FormData();
    formData.append("file", file);

    // Create preview URL
    const url = URL?.createObjectURL(file);
    setPreviewUrl(url);

    try {
      const res = await dispatch(saveFile(formData));
      if (res?.payload?.success) {
        setProfile(res?.payload?.data?.result ?? "");
      } else {
        toast.error("Please try upload your image again");
      }
    } catch (error) {
      toast.error("An error occurred while uploading the image");
    } finally {
      setLoadingAttach(false);
    }
  };

  const [image, setImage] = useState(null);

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setImage(file);
      setPreview(URL?.createObjectURL(file));
      handleImageSelect(e);
    }
  };

  function handleSelectChange(value, formName) {
    setFormData((prevData) => ({
      ...prevData,
      [formName]: value,
    }));
  }
 
  const onFinish = async (data) => {
    // if (!profile) {
    //   toast.error("Attach your photo");
    //   return;
    // }
    if (!formData?.rsBusinessType) {
      toast.error("Please select a business type");
      return;
    }
    const res = await dispatch(
      save({
        url: "api/v2/res",
        rsId: prodd?.rsId ? prodd?.rsId : null,
        rsSmsUnitPrice: formData?.rsSmsUnitPrice,
        rsCompanyName: formData?.rsCompanyName,
        rsEmail: formData?.rsEmail,
        rsPhoneNumber: formData?.rsPhoneNumber,
        rsContactPerson: formData?.rsContactPerson,
        vatNumber: formData?.vatNumber,
        rsBusinessType: formData?.rsBusinessType,
        raWebsite: formData?.raWebsite,
        raCity: formData?.raCity,
        raState: formData?.raState,
        raPostalCode: formData?.raPostalCode,
        rsMsgBal:formData?.rsMsgBal,
        rsLogo: profile,
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await fetchResellerData();
      await form.resetFields();
      await setPreview(null);
      await setIsModalOpen(false);
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  // useEffect(() => {
  //   if (prodd?.rsId) {
  //     dispatch(
  //       fetchResellerImage({
  //         rsId: prodd?.rsId,
  //       })
  //     );
  //   }
  // }, [prodd]);

  return (
    <>
      <Modal
        className=""
        title={`${
          prodd?.rsId ? `Update ${prodd?.rsCompanyName}` : "New Reseller"
        }`}
        open={isModalOpen}
        onOk={onFinish}
        onCancel={handleCancel}
        width={850}
        maskClosable={false}
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
          {/* <Form.Item
            label={
              <span>
                Reseller Logo<span className="text-[#FF0000]"></span>
              </span>
            }
          >
            {loading ? (
              <Spin />
            ) : (
              <div className="items-center flex gap-5  ">
                {prodd?.rsId ? (
                  <>
                    {(!resellerImage && !previewUrl) ? (
                      <label className="flex flex-row items-center justify-start w-[20%]">
                        <input
                          name="secIcon"
                          type="file"
                          accept="image/*"
                          style={{ display: "none" }}
                          onChange={(e) => handleImageSelect(e)}
                        />
                        <img
                          loading="lazy"
                          className="z-10 w-[100px] h-[100px] rounded-full"
                          alt="tab1"
                          src={uplodSvg}
                          style={{ cursor: "pointer" }}
                        />
                      </label>
                    ) : loadingAttach ? (
                      <Spin className="sms-spin" />
                    ) : (
                      <label className="flex flex-row items-center justify-start gap-2  w-[20%]">
                        <input
                          name="secIcon"
                          type="file"
                          accept="image/*"
                          loading="lazy"
                          style={{ display: "none" }}
                          onChange={handleImageSelect}
                        />
                        <div className="overflow-hidden">
                          <img
                            src={
                              previewUrl
                                ? previewUrl
                                : `${url}/api/v2/public/view-logo/${prodd?.rsId}`
                            }
                            loading="lazy"
                            className="w-28 h-28 cursor-pointer object-cover"
                            alt="Profile3"
                          />
                        </div>
                      </label>
                    )}
                  </>
                ) : (
                  <label>
                    <input
                      name="secIcon"
                      style={{ display: "none" }}
                      type="file"
                      accept="image/*"
                      onChange={handleImageChange}
                    />
                    <div className="">
                      <img
                        src={preview ? preview : uplodSvg}
                        alt="Image Preview"
                        className="w-28 h-28 object-cover rounded-full"
                      />
                    </div>
                  </label>
                )}
              </div>
            )}
          </Form.Item> */}
          <div className="grid lg:grid-cols-2 grid-cols-1 lg:gap-x-5 lg:gap-y-0 gap-y-5">
            <Form.Item
              label={
                <span>
                  Unit Price(KES)<span className="text-[#FF0000]">*</span>
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
                type="number"
                name="rsSmsUnitPrice"
                onChange={onChange}
                value={formData?.rsSmsUnitPrice}
                className="input"
              />
            </Form.Item>
            <Form.Item
              label={
                <span>
                  Company Name<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add company name",
                },
              ]}
            >
              <Input
                required
                name="rsCompanyName"
                onChange={onChange}
                value={formData?.rsCompanyName}
                className="input"
              />
            </Form.Item>
            <Form.Item
              label={
                <span>
                  Email<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add email address",
                },
                {
                  type: "email",
                  message: "Please enter a valid email address",
                },
              ]}
            >
              <Input
                required
                name="rsEmail"
                onChange={onChange}
                value={formData?.rsEmail}
                type="email"
                className="input"
              />
            </Form.Item>

            <Form.Item
              label={
                <span>
                  Phone No.<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add office phone no",
                },
              ]}
            >
              <PhoneInput
                required
                name="rsPhoneNumber"
                onChange={handleNumberChange}
                value={formData?.rsPhoneNumber}
                country="ke"
                onlyCountries={["ke"]}
                countryCodeEditable={false}
                className="input rounded-[6px] border !border-[#cacaca] !h-[42px]"
              />
            </Form.Item>

            <Form.Item
              label={
                <span>
                  Contact Person<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add contact person",
                },
              ]}
            >
              <Input
                required
                name="rsContactPerson"
                onChange={onChange}
                value={formData?.rsContactPerson}
                className="input"
              />
            </Form.Item>

            <Form.Item
              label={
                <span>
                  VAT Number<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add Vat number",
                },
              ]}
            >
              <Input
                required
                name="vatNumber"
                onChange={onChange}
                value={formData?.vatNumber}
                className="input"
              />
            </Form.Item>

            <Form.Item
              className=""
              label={
                <span>
                  Business Type <span className="text-[#FF0000]">*</span>
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
                name="rsBusinessType"
                className=""
                allowClear
                style={{
                  width: "100%",
                }}
                onChange={(value) => {
                  handleSelectChange(value, "rsBusinessType");
                }}
                value={formData?.rsBusinessType}
                options={
                  instTypeData?.length > 0 &&
                  instTypeData?.map((item) => ({
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
              label="Website"
              rules={[
                {
                  required: true,
                  message: "Please add website",
                },
                {
                  type: "url",
                  message: "Please add a valid website URL",
                },
              ]}
            >
              <Input
                required
                name="raWebsite"
                value={formData?.raWebsite}
                onChange={onChange}
                className="input"
              />
            </Form.Item>

            <Form.Item
              label={
                <span>
                  City<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add city",
                },
              ]}
            >
              <Input
                required
                name="raCity"
                onChange={onChange}
                value={formData?.raCity}
                className="input"
              />
            </Form.Item>

            <Form.Item
              label={
                <span>
                  State<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add state",
                },
              ]}
            >
              <Input
                required
                name="raState"
                onChange={onChange}
                value={formData?.raState}
                className="input"
              />
            </Form.Item>

            <Form.Item
              label={
                <span>
                  Postal Code<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add postal code",
                },
              ]}
            >
              <Input
                required
                name="raPostalCode"
                onChange={onChange}
                value={formData?.raPostalCode}
                className="input"
              />
            </Form.Item>
            <Form.Item
              label="Balance"  
              rules={[
                {
                  required: false,
                  message: "Please add Balance",
                },
              ]}
            >
              <InputNumber
                formatter={(value) =>
                  `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ",")
                }
                parser={(value) => value?.replace(/\$\s?|(,*)/g, "")}
                required
                name="rsMsgBal"
                onChange={(value) => onChangeOther(value, "rsMsgBal")} 
                value={formData?.rsMsgBal}
                className="input flex"
              />
            </Form.Item>
            
          </div>

          <div className="flex justify-between mt-[48px] ">
            <div className="justify-start"></div>
            <div className="justify-end flex items-center mb-[58px] gap-x-5 cursor-pointer">
              <button
                key="back"
                type="button"
                onClick={handleCancel}
                className="cstm-btn !bg-white !text-[#388E3C] !border !border-[#388E3C]"
              >
                Cancel
              </button>

              <div className="w-[150px]">
                <button
                  key="submit"
                  type="submit"
                  className="cstm-btn"
                  disabled={saving}
                >
                  {saving ? <Spin /> : prodd?.rsId ? "Update" : "Create"}
                </button>
              </div>
            </div>
          </div>
        </Form>
      </Modal>
    </>
  );
};
export default ResellerAddModal;
