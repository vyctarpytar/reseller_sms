import React, { useEffect, useRef, useState } from "react";
import { DatePicker, Form, Input, Modal, Spin } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { save } from "../../features/save/saveSlice";
import { disabledPastDate } from "../../utils";
import dayjs from "dayjs";

const { TextArea } = Input;
const RescheduleModal = ({ open, handleCancel, prodd, handleFetch }) => {
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const { saving } = useSelector((state) => state.save);

  const linkRef = useRef(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [data, setdata] = useState({});

  const onOk = (value) => {};

  const [dateSchedule, setDateSchedule] = useState("");

  const onFinish = async (data) => {
    data.schId = prodd?.schId;
    const res = await dispatch(
      save({
        url: "api/v2/schedule/update",
        ...data,
        schReleaseTime: data.schReleaseTime
          ? data.schReleaseTime.format("YYYY-MM-DD HH:mm")
          : null,
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await form.resetFields();
      await handleFetch();
      await handleCancel();
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  useEffect(() => {
    if (prodd) {
      form.setFieldsValue({
        ...prodd,
        schReleaseTime: prodd?.schReleaseTime
          ? dayjs(new Date(prodd?.schReleaseTime))
          : null,
      });
    } else {
      form.resetFields();
    }
  }, [prodd, form]);

  return (
    <>
      <Modal
        className=""
        title={`Reschedule sms for ${prodd?.schGroupName}`}
        open={open}
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
          <Form.Item
            extra={"Choose date & time"}
            label={
              <span>
                Pick date & Time <span className="text-[#FF0000]">*</span>
              </span>
            }
            name="schReleaseTime"
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
                 format={"YYYY-MM-DD HH:mm"}
              placeholder="Select Date"
              className="mr-3 border border-black"
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
              onChange={(value, dateString) => {
                setDateSchedule(dateString);
              }}
              onOk={onOk}
            />
          </Form.Item>
          <Form.Item
            name="schMessage"
            label="Message"
            rules={[
              {
                required: true,
                message: "Enter message of this group",
              },
            ]}
            extra={"Write a brief message of this group"}
          >
            <TextArea rows={4} className="input-textarea" />
          </Form.Item>

          <div className="flex justify-between mt-[48px] ">
            <div className="justify-start"></div>
            <div className="justify-end flex items-center mb-[58px] gap-x-3">
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
                  {saving ? <Spin /> : "Update"}
                </button>
              </div>
            </div>
          </div>
        </Form>
      </Modal>
    </>
  );
};
export default RescheduleModal;
