import React, { useEffect, useRef, useState } from "react";
import {DatePicker, Form, Input, Modal, Spin } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import {
  disabledPastDate,
  formatDate,
} from "../../utils";
import { save } from "../../features/save/saveSlice"; 
import {  fetchSingleAccount } from "../../features/reseller-account/resellerAccountSlice"; 

const { TextArea } = Input;
const DisableModal = ({ isModalOpen, setIsModalOpen, prodd ,title}) => { 
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

 
  const [data, setdata] = useState({});

 
  async function fetchSingleAccountData() {
    dispatch(
      fetchSingleAccount({
        accId: prodd?.accId,
      })
    );
  }

  const onFinish = async (data) => {
 
    const res = await dispatch(
      save({
        url: `api/v2/account/disable/${prodd?.accId}`, 
        accDisableDate:formatDate(data?.accDisableDate),
        accToDiableTimer:data?.accToDiableTimer
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await fetchSingleAccountData()
      form.resetFields();
      await setIsModalOpen(false);
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
                extra={"Choose date"}
                label="Date to Disable"
                name="accDisableDate"
                rules={[
                  {
                    required: true,
                    message: "Disable Date is required",
                  },
                ]}
                className="mr-6 w-full"
              >
                <DatePicker
                  style={{
                    width: "100%",
                    height: "52px",
                  }}
                  placeholder="Select Date"
                  className="mr-3 border border-black" 
                  format={"YYYY-MM-DD"}
                  disabledDate={disabledPastDate}
                />
              </Form.Item>

              <Form.Item
               extra={"Timer (days)"}
               name="accToDiableTimer" 
              label={
                <span>
                  Timer(days)<span className="text-[#FF0000]">*</span>
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
                type="number" 
                className="input"
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
export default DisableModal;
