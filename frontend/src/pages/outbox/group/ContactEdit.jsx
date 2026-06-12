import React, { useEffect, useRef, useState } from "react";
import InsideHeader from "../../../components/InsideHeader";
import { Button, DatePicker, Form, Input, Select, Spin } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate, useParams } from "react-router-dom";
import { save } from "../../../features/save/saveSlice"; 
import toast from "react-hot-toast";
import { GenderData } from "../../../data";
import { disabledDate } from "../../../utils";
import PhoneInput from "react-phone-input-2";
import moment from "moment/moment";
import { fetchSingleContacts } from "../../../features/contact/ContactSlice";
 

const { TextArea } = Input;
function ContactEdit() {
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const { user } = useSelector((state) => state.auth);
  const { folderObj } = useSelector((state) => state.folder);
  const { saving } = useSelector((state) => state.save);
  const {singleContact} =  useSelector((state) => state.contact);

  const {id} =  useParams();
   

  
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [formData, setFormData] = useState(singleContact);

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
      chTelephone: String(e),
    }));
  };



  const onChange = async (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };
  function onMonthChange(value) {
    setFormData((prevData) => ({
      ...prevData,
      chDob: value,
    }));
  }

  const handleCancel = async (e) => {
    navigate(-1);
  };

  async function handlefetchSingleContactsData() {
    await dispatch(fetchSingleContacts(id));
  }
  const onFinish = async (data) => {
    if (!formData?.chGenderCode) {
      toast.error("Please select gender");
      return;
    }
    if (!formData?.chDob) {
      toast.error("Please select date of birth");
      return;
    }
    const res = await dispatch(
      save({
        url: "api/v2/member",
        chId:id,
        chGroupId: folderObj?.groupId, 
        chFirstName: formData?.chFirstName, 
        chOtherName:formData?.chOtherName,
        chGenderCode: formData?.chGenderCode,
        chDob: formData?.chDob,
        chNationalId: formData?.chNationalId, 
        chTelephone: formData?.chTelephone,
        chOption1: formData?.chOption1,
        chOption2: formData?.chOption2,
        chOption3: formData?.chOption3,
        chOption4: formData?.chOption4,
      })
    );
    if (res?.payload?.success) {
     await toast.success(res?.payload?.messages?.message);
      await navigate(`/contacts/folder/${folderObj?.groupName}`); 
      form.resetFields();
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  useEffect(()=>{
    handlefetchSingleContactsData()
  },[id])
  
  useEffect(() => {
    setFormData(singleContact);
  }, [singleContact]);
  return (
    <>
       <div className="w-full overflow-y-scroll h-full">
        <InsideHeader
          title="Contacts"
          subtitle="Edit individual members in the group"
          back={true}
        />

        <div className="lg:px-10 px-3">
          <div className="mt-10 flex flex-col">
            <div className="product_request_title !text-[31px]">
              Member Form
            </div>
            <div className="product_sub  mt-[0.5rem]">
              Edit form with the member details.
            </div>

            <Form
              layout="vertical"
              ref={formRef}
              name="control-ref"
              onFinish={onFinish}
              className="mt-10"
              style={{
                maxWidth: "100%",
              }}
              form={form}
            >
              <div className="grid lg:grid-cols-2 grid-cols-1 lg:gap-x-5 lg:gap-y-0 gap-y-5">
              
                 
                <Form.Item
                  label={
                    <span>
                      First Name<span className="text-[#FF0000]">*</span>
                    </span>
                  }
                  rules={[
                    {
                      required: true,
                      message: "Please add full name",
                    },
                  ]}
                >
                  <Input
                    required
                    name="chFirstName"
                    onChange={onChange}
                    value={formData?.chFirstName}
                    className="input"
                  />
                </Form.Item> 
                <Form.Item
                  label={
                    <span>
                    Other Names<span className="text-[#FF0000]">*</span>
                    </span>
                  }
                  rules={[
                    {
                      required: true,
                      message: "Please add full name",
                    },
                  ]}
                >
                  <Input
                    required
                    name="chOtherName"
                    onChange={onChange}
                    value={formData?.chOtherName}
                    className="input"
                  />
                </Form.Item> 

                <Form.Item
                  className=""
                  label={
                    <span>
                      Gender <span className="text-[#FF0000]">*</span>
                    </span>
                  }
                  rules={[
                    {
                      required: true,
                      message: "Please select gender",
                    },
                  ]}
                >
                  <Select
                    name="chGenderCode"
                    className=""
                    allowClear
                    style={{
                      width: "100%",
                    }}
                    onChange={(value) => {
                      handleSelectChange(value, "chGenderCode");
                    }}
                    value={formData?.chGenderCode}
                    options={
                      GenderData?.length > 0 &&
                      GenderData?.map((item) => ({
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

                <Form.Item label="Date of birth" className="mr-6 w-full">
                  <DatePicker
                    name="chDob"
                    style={{
                      width: "100%",
                      height: "42px",
                    }}
                    placeholder={
                      formData?.chDob
                        ? moment(formData?.chDob)?.format("DD-MM-YYYY")
                        : "Select Date"
                    }
                    className="mr-3"
                    format={"DD-MM-YYYY"}
                    onChange={onMonthChange}
                    disabledDate={disabledDate}
                  />
                </Form.Item>

                <Form.Item
                  label={
                    <span>
                      National ID<span className="text-[#FF0000]">*</span>
                    </span>
                  }
                  rules={[
                    {
                      required: true,
                      message: "Please add national ID",
                    },
                  ]}
                >
                  <Input
                    required
                    type="number"
                    name="chNationalId"
                    onChange={onChange}
                    value={formData?.chNationalId}
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
                      message: "Please add phone no",
                    },
                  ]}
                >
                  <PhoneInput
                    required
                    name="chTelephone"
                    onChange={handleNumberChange}
                    value={formData?.chTelephone}
                    country="ke"
                    onlyCountries={["ke"]}
                    countryCodeEditable={false}
                    className="input rounded-[6px] border !border-[#cacaca] !h-[42px]"
                  />
                </Form.Item>

                <Form.Item
                  label={
                    <span>
                      Option 1 
                    </span>
                  }
                  rules={[
                    {
                      required: false,
                      message: "Please add full name",
                    },
                  ]}
                >
                  <Input 
                    name="chOption1"
                    onChange={onChange}
                    value={formData?.chOption1}
                    className="input"
                  />
                </Form.Item> 

                <Form.Item
                  label={
                    <span>
                      Option 2 
                    </span>
                  }
                  rules={[
                    {
                      required: false,
                      message: "Please add full name",
                    },
                  ]}
                >
                  <Input 
                    name="chOption2"
                    onChange={onChange}
                    value={formData?.chOption2}
                    className="input"
                  />
                </Form.Item> 
                <Form.Item
                  label={
                    <span>
                      Option 3 
                    </span>
                  }
                  rules={[
                    {
                      required: false,
                      message: "Please add full name",
                    },
                  ]}
                >
                  <Input 
                    name="chOption3"
                    onChange={onChange}
                    value={formData?.chOption3}
                    className="input"
                  />
                </Form.Item> 
                <Form.Item
                  label={
                    <span>
                      Option 4 
                    </span>
                  }
                  rules={[
                    {
                      required: false,
                      message: "Please add full name",
                    },
                  ]}
                >
                  <Input 
                    name="chOption4"
                    onChange={onChange}
                    value={formData?.chOption4}
                    className="input"
                  />
                </Form.Item> 

 
              </div>


              
              <div className="flex justify-between mt-[48px] ">
                <div className="justify-start"></div>
                <div className="justify-end flex items-center mb-[58px] gap-x-5">
                <div className="w-[150px]">
                <button
                  key="back"
                  type="button"
				          onClick={handleCancel}
                  className="cstm-btn !bg-white !text-[#388E3C] !border !border-[#388E3C]"
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
                      {saving ? <Spin /> : "Update"}
                    </button>
                  </div>
                </div>
              </div>
            </Form>
          </div>
        </div>
      </div>
    </>
  );
}

export default ContactEdit;
