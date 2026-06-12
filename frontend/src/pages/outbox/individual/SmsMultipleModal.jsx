import React, { useEffect, useRef, useState } from "react";
import { Button, Form, Input, Modal, Select, Spin, Tooltip } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast"; 
import { save } from "../../../features/save/saveSlice";
import svg43 from '../../../assets/svg/svg43.svg'
import "./mention.css";
import { fetchDistinctSenderNames } from "../../../features/filter/filterSlice";
import ScheduleMultipleModal from "../group/modal/ScheduleMultipleModal";

const { TextArea } = Input;
const SmsMultipleModal = ({
  isModalOpen,
  setIsModalOpen, 
  dataSource,
}) => {
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
 
  const onChange = async (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  const [isModalSchedule, setIsModalSchedule] = useState(false);
  const showModalConfirmation = async () => { 
    if (!formData?.message ) {
      toast.error("Please enter message");
      return;
    }
    if(!formData?.senderId){
      toast.error("Please select sender ID")
      return;
    }
    setIsModalSchedule(true);
  };

  const filteredPhones = dataSource
    ?.filter((item) => item?.phone !== "Click to add contact")
    ?.map((item) => item?.phone); 
  const onFinish = async (data) => {
    const res = await dispatch(
      save({
        url: "api/v2/sms/single-sms",
        message: formData?.message,
        mobile: filteredPhones?.join(","),
        senderId: formData?.senderId,
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await form.resetFields();
      await setIsModalOpen(false);
      await navigate("/sent-sms");
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

  useEffect(() => {
    fetchDistinctSenderNamesData();
  }, []);

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

          <Form.Item 
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
            required
             name="message"
             value={formData?.message}
              onChange={onChange}
              placeholder="Please add a comment"
              rows={4}
              className="input-textarea"
            />
          </Form.Item>

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

              <div className="flex">
                <div className="w-[150px]">
                  <button
                    key="submit"
                    type="submit"
                    className="cstm-btn  !rounded-tr-[0px] !rounded-br-[0px]"
                    disabled={saving}
                  >
                    {saving ? <Spin /> : "Send"}
                  </button>
                </div>
                <div className="w-[65px]">
                  <Button
                    onClick={showModalConfirmation}
                    className="cstm-btn   !bg-[#739675] !rounded-tl-[0px] !rounded-bl-[0px]  !h-[2.6rem] "
                  >
                    {saving ? (
                      <Spin />
                    ) : (
                      <Tooltip title="Schedule send">
                        <img className="" src={svg43} alt="svg43" />
                      </Tooltip>
                    )}
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </Form>
      </Modal>
      

      <ScheduleMultipleModal
        isModalOpen={isModalSchedule}
        setIsModalOpen={setIsModalSchedule}
        title={`Schedule Send`} 
        setIsModalOpenPrev={setIsModalOpen}
        message={formData?.message}
        mobile={filteredPhones?.join(",")}
        senderId={formData?.senderId} 
      />
    </>
  );
};
export default SmsMultipleModal;
