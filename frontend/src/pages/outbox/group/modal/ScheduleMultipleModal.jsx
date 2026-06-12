import React, { useEffect, useRef, useState } from "react";
import {
  DatePicker,
  Descriptions,
  Divider,
  Form,
  Input,
  Modal,
  Select,
  Spin,
} from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { save } from "../../../../features/save/saveSlice";
import {
  getTomorrowDateAfternoon,
  getTomorrowDateMorning,
} from "../../../../utils";
import dayjs from "dayjs";

const { TextArea } = Input;
const ScheduleMultipleModal = ({
  isModalOpen,
  setIsModalOpen,
  setInputValue,
  title,
  setIsModalOpenPrev,
  message,
  mobile,
  senderId,
}) => {
  const handleOk = () => {
    setIsModalOpen(false);
  };

  const [form] = Form.useForm();
  const formRef = useRef(null);
  const { user } = useSelector((state) => state.auth);
  const { saving } = useSelector((state) => state.save);
  const { invoDistinctStatus } = useSelector((state) => state.inv);
  const { folderObj } = useSelector((state) => state.folder);

  const linkRef = useRef(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [data, setdata] = useState({});

  const onOk = (value) => {};

  const [dateSchedule, setDateSchedule] = useState("");
  const [activeSchedule, setActiveSchedule] = useState("");
  const [showDate, setShowDate] = useState(false);

  const handleCancel = async () => {
    await setIsModalOpen(false);
    await setShowDate(false);
    await setActiveSchedule(false);
  };

  const onFinish = async (data) => {
    if (!dateSchedule) {
      toast.error("Select date & time");
      return;
    }
    const res = await dispatch(
      save({
        url: "api/v2/sms/single-sms",
        message: message,
        mobile: mobile,
        senderId: senderId,
        sendAt: dateSchedule,
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await form.resetFields();
      await setIsModalOpen(false);
      await setIsModalOpenPrev(false);
      await navigate("/sent-sms");
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  return (
    <>
      <Modal
        className=""
        title={title}
        open={isModalOpen}
        onOk={onFinish}
        onCancel={handleCancel}
        width={500}
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
          {showDate ? (
            <Form.Item
              extra={"Choose date & time"}
              label={
                <span>
                  Pick date & Time <span className="text-[#FF0000]">*</span>
                </span>
              }
              name="invoMarkedPaidValueDate"
              rules={[
                {
                  required: true,
                  message: "Inoice Date is required",
                },
              ]}
              className="mr-6 w-full"
            >
              <DatePicker
                showTime
                style={{
                  width: "100%",
                  height: "52px",
                }}
                placeholder="Select Date"
                className="mr-3 border border-black"
                format={"YYYY-MM-DD HH:mm"}
                onChange={(value, dateString) => {
                  setDateSchedule(dateString);
                }}
                disabledDate={(current) => {
                  return current && current < dayjs().startOf("day");
                }}
                disabledTime={(current) => {
                  if (!current) return {};
                  const now = dayjs();
                  if (current?.isSame(now, "day")) {
                    return {
                      disabledHours: () => [...Array(now?.hour())?.keys()],
                      disabledMinutes: (hour) =>
                        hour === now?.hour()
                          ? [...Array(now?.minute())?.keys()]
                          : [],
                      disabledSeconds: () => [],
                    };
                  }
                  return {};
                }}
                onOk={onOk}
              />
            </Form.Item>
          ) : (
            <>
              <Form.Item name="invoMarkedPaidValueDate" className="mr-6 w-full">
                <div
                  className={`text-[#3c4043] flex items-center justify-between font-dmSans text-[16px] cursor-pointer
                            ${activeSchedule === "morning" && "bg-[#f0f0f0]"}`}
                  onClick={() => {
                    setDateSchedule(getTomorrowDateMorning());
                    setActiveSchedule("morning");
                  }}
                >
                  <span>Tomorrow morning</span>
                  <span className="flex items-start">
                    {getTomorrowDateMorning()} Am
                  </span>
                </div>

                <div
                  className={`text-[#3c4043] flex items-center justify-between font-dmSans text-[16px] mt-3 cursor-pointer
                         ${activeSchedule === "afternoon" && "bg-[#f0f0f0]"}`}
                  onClick={() => {
                    setDateSchedule(getTomorrowDateAfternoon());
                    setActiveSchedule("afternoon");
                  }}
                >
                  <span>Tomorrow afternoon</span>
                  <span className="flex items-start text-start justify-start">
                    {getTomorrowDateAfternoon()} Pm
                  </span>
                </div>
              </Form.Item>

              <Divider className="mb-1" />
              <div
                className="text-[#3c4043] flex font-dmSans text-[16px] cursor-pointer"
                onClick={() => setShowDate(true)}
              >
                Pick date & time
              </div>
            </>
          )}

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
                  {saving ? <Spin /> : "Schedule"}
                </button>
              </div>
            </div>
          </div>
        </Form>
      </Modal>
    </>
  );
};
export default ScheduleMultipleModal;
