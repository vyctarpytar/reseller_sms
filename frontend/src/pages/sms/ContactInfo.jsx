import { Checkbox, Form, Input, Select, message } from "antd";
import React, { useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import { IdTypeData, instTypeData } from "../../data";
import { cleanCurrent } from "../../features/sms-request/smsRequestSlice";
 
 
const { TextArea } = Input;
function ContactInfo({ next, prev }) {
  const [form] = Form.useForm();
  const formRef = useRef(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [data, setData] = useState({});
 

  function handleSelectChange(value, formName) {
    setData((prevData) => ({
      ...prevData,
      [formName]: value,
    }));
  }
 async function clean(){
  await dispatch(cleanCurrent())
 }
   

  const onFinish = (data) => {
    clean();
  };
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
          
            <Form.Item
              name="usrGender"
              className="login-form-item"
              label={
                <span>
                  Are you a company or an individual? <span className="text-[#FF0000]">*</span>
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
                  handleSelectChange(value, "instType");
                }}
                options={instTypeData}
              />
            </Form.Item>

            

            <div className="mt-[20px]  lg:mb-0 mb-[20px] flex lg:px-0 px-3   justify-end  gap-x-4">
              
              <div className="w-[109px]">
                <button type="submit" key="submit" className={`cstm-btn`}>
                  Next
                </button>
              </div>
            </div> 
        </Form>
      </div>
    </>
  );
}

export default ContactInfo;
