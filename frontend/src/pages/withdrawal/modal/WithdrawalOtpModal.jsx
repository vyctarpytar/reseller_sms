import React, { useEffect, useRef, useState } from "react";
import { Form, Input, Modal, Spin } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { fetchMyProfile } from "../../../features/auth/authSlice";
import { addSpaces } from "../../../utils"; 
import { save } from "../../../features/save/saveSlice";

const WithdrawalOtpModal = ({ isModalOpen, setIsModalOpen, prodd }) => {
  const handleOk = () => {
    setIsModalOpen(false);
  };
  const handleCancel = () => {
    setIsModalOpen(false);
  };
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const { userProfile, user } = useSelector((state) => state.auth);
  const {   withdrawalData } = useSelector((state) => state.billing);
  const { saving } = useSelector((state) => state.save);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  function closeModal() {
    toast.dismiss();
  }

  const [formData, setFormData] = useState({});
  const [isInputEmpty, setIsInputEmpty] = useState(true);

  const handleInputChange = (e) => {
    const value = e.target.value;
    setIsInputEmpty(!value);
    setFormData((prevData) => ({
      ...prevData,
      usrSecret: value,
    }));
  };

  const onFinish = async (data) => {
    const res = await dispatch(
      save({
        withDrawAmount: withdrawalData?.withDrawAmount,
        withDrawPhoneNumber: withdrawalData?.withDrawPhoneNumber,
        withDrawLogId: withdrawalData?.withDrawLogId,
        withDrawCode: data?.usrSecret,
        url: "api/v2/wallet/finalize-withdraw",
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await form.resetFields()
      // await setIsModalOpen(false);
      await navigate('/withdrawal')
    } else {
      await toast.error(res?.payload?.messages?.message);
      await form.resetFields()
      await setIsModalOpen(false);
      await navigate('/withdrawal')
    }
  };

  async function fetchMyUsersData() {
    await dispatch(fetchMyProfile());
  }

  useEffect(() => {
    fetchMyUsersData();
  }, []);

  console.log("withdrawalData", withdrawalData);

  return (
    <>
      <Modal
        className=""
        title="Withdraw Money"
        open={isModalOpen}
        onOk={onFinish}
        onCancel={handleCancel}
        width={800}
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
            name="usrSecret"
            label={
              <span>
                Enter OTP sent to{" "}
                {userProfile?.phoneNumber
                  ? addSpaces(userProfile?.phoneNumber)
                  : ""}{" "}
                <span className="text-[#FF0000]">*</span>
              </span>
            }
            className="login-form-item "
            rules={[
              {
                required: true,
                message: "Please add otp",
              },
            ]}
          >
            <Input onChange={handleInputChange} className="input" />
          </Form.Item>

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
                <button key="submit" type="submit" className="cstm-btn">
                  {saving ? <Spin /> : "Withdraw"}
                </button>
              </div>
            </div>
          </div>
        </Form>
      </Modal>
    </>
  );
};
export default WithdrawalOtpModal;
