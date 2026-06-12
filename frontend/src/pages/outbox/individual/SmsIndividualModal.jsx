import React, { useEffect, useRef, useState } from "react";
import { Form, Input, Modal, Select, Spin } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import PhoneInput from "react-phone-input-2";
import { save } from "../../../features/save/saveSlice";
import { addSpaces } from "../../../utils";
import MessageTemplate from "../../mesage-template/MessageTemplate";
import "./mention.css";
import { fetchDistinctSenderNames } from "../../../features/filter/filterSlice";

const { TextArea } = Input;
const SmsIndividualModal = ({ isModalOpen, setIsModalOpen, rowId }) => {
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
  const { senderNamesData } = useSelector((state) => state.filterSms);

  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [formData, setFormData] = useState();
  const [data, setData] = useState({});
  const handleNumberChange = (e) => {
    setData((prevData) => ({
      ...prevData,
      mobile: String(e),
    }));
  };

  const onFinish = async (data) => {
    const res = await dispatch(
      save({
        url: "api/v2/sms/single-sms",
        message: data?.message,
        mobile: rowId?.length > 0 ? rowId?.[0]?.chTelephone : data?.mobile,
        senderId:formData?.senderId
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await form.resetFields();
      await setIsModalOpen(false);
      await navigate("/sent-sms")
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };
 
  async function handleSelectChange(value, formName) { 
    await setFormData((prevData) => ({
       ...prevData,
       [formName]: value,
     })); 
   }

   function fetchDistinctSenderNamesData() {
    dispatch(fetchDistinctSenderNames());
  }

  useEffect(()=>{
    fetchDistinctSenderNamesData()
  },[])
  
  return (
    <>
      <Modal
        className=""
        title="Message"
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
              <Form.Item
              name="senderId"
              className=""
              label={
                <span>
                  Select sender name <span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please select sender name",
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
                  handleSelectChange(value, "senderId");
                }}
                options={
                  senderNamesData?.length > 0 &&
                  senderNamesData?.map((item) => ({
                    value: item,
                    label: item,
                  }))
                }
                showSearch
                optionFilterProp="children"
                filterOption={(input, option) =>
                  (option?.label?.toLocaleLowerCase() ?? "").includes(
                    input?.toLocaleLowerCase()
                  )
                }
                onDropdownVisibleChange={() => {
                  fetchDistinctSenderNamesData();
                }}
              />
            </Form.Item>
          {rowId?.length > 0 ? (
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
              <Input
                readOnly={true}
                name="mobile"
                value={addSpaces(rowId?.[0]?.chTelephone)}
                className="input rounded-[6px] border !border-[#cacaca] !h-[42px]"
              />
            </Form.Item>
          ) : (
            <Form.Item
              name="mobile"
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
                onChange={handleNumberChange}
                country="ke"
                onlyCountries={["ke"]}
                countryCodeEditable={false}
                className="input rounded-[6px] border !border-[#cacaca] !h-[42px]"   
              />
            </Form.Item>
          )}

          <Form.Item
            name="message"
            label={
              <span>
                Custom Message<span className="text-[#FF0000]">*</span>
              </span>
            }
            rules={[
              {
                required: true,
                message: "Please add a comment",
              },
            ]}
          >
            <TextArea
              placeholder="Please add a comment"
              rows={4}
              className="input-textarea"
            />
          </Form.Item>

          {/* <div className="mention-container">
            <Form.Item
              name="message 2"
              label={
                <span>
                  Custom Message <span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add a comment",
                },
              ]}
            >
              <TextArea
                placeholder="Please add a comment"
                rows={4}
                className="input-textarea"
                ref={inputRef}
                value={inputValue}
                onChange={handleInputChange}
              />
            </Form.Item>
    

            {dropdownVisible && (
              <div className="dropdown">
                {dropdownItems.map((item) => (
                  <div
                    key={item}
                    className="dropdown-item"
                    onClick={() => handleDropdownClick(item)}
                  >
                    {item}
                  </div>
                ))}
              </div>
            )}
          </div> */}

         

          <div className="flex justify-between mt-[48px] ">
            <div className="justify-start"></div>
            <div className="justify-end flex items-center mb-[58px] gap-x-5">
              <div className="w-[150px] ">
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
                  {saving ? <Spin /> : "Send Sms"}
                </button>
              </div>
            </div>
          </div>
        </Form>
      </Modal>
    </>
  );
};
export default SmsIndividualModal;
