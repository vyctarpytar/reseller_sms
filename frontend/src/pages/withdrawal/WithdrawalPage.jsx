import { Form, InputNumber, Spin } from "antd";
import React, { useRef, useState } from "react";
import toast from "react-hot-toast";
import PhoneInput from "react-phone-input-2";
import { useNavigate } from "react-router-dom";
import mpesa from "../../assets/svg/mpesa.jpeg";

import WithdrawalOtpModal from "./modal/WithdrawalOtpModal"; 
import { useDispatch, useSelector } from "react-redux";
import { saveWithdrawal } from "../../features/billing/billingSlice";
import InsideHeader from "../../components/InsideHeader";

function WithdrawalPage() {
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const navigate = useNavigate();
  const dispatch =  useDispatch();

  const {saving} =  useSelector((state)=>state.billing)

  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };

  const [formData, setFormData] = useState();
  const onChangeOther = (value, fieldName) => {
    setFormData((prev) => ({
      ...prev,
      [fieldName]: value,
    }));
  };

  const handleCancel = () => {
    navigate("/withdrawal");
  };
  const handleNumberChange = (e) => {
    setFormData((prevData) => ({
      ...prevData,
      withDrawPhoneNumber: String(e),
    }));
  };

  const onFinish = async (data) => {
    if (!formData?.withDrawAmount) {
      toast.error("Please add withdrawal amount");
      return;
    } 
    if (!formData?.withDrawPhoneNumber) {
      toast.error("Please add withdrawal phone number");
      return;
    }
   
    const res = await dispatch(
      saveWithdrawal({
        withDrawAmount: formData?.withDrawAmount,
        withDrawPhoneNumber: formData?.withDrawPhoneNumber, 
        url: "api/v2/wallet/initiate-withdraw",
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await showModal()
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };
  return (
    <div className="w-full overflow-y-scroll h-full">
      <InsideHeader
        title="Withdrawal"
        subtitle="Withdraw your earnings here"
        back={true}
      /> 
      <div className="lg:p-10 p-3">
      <Form
        layout="vertical"
        ref={formRef}
        name="control-ref"
        onFinish={onFinish}
        className="lg:w-[700px] w-full lg:mt-0 mt-5"
        style={{
          maxWidth: "100%",
        }}
        form={form}
      >
        <Form.Item
          label="Enter amount to withdraw"
          rules={[
            {
              required: true,
              message: "Enter amount to withdraw",
            },
          ]}
        >
          <InputNumber
            name="withDrawAmount"
            value={formData?.withDrawAmount}
            min={0}
            formatter={(value) =>
              `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ",")
            }
            parser={(value) => value?.replace(/\$\s?|(,*)/g, "")}
            onChange={(value) => onChangeOther(value, "withDrawAmount")}
            placeholder="Enter amount to withdraw"
            className="input flex"
          />
        </Form.Item>

        <Form.Item label="Withdraw to">
          <div>
            <img src={mpesa} loading="lazy" alt="mpesa" className="w-[100px] h-full"/>
          </div>
        </Form.Item>
        <Form.Item
          label="Enter mpesa number"
          rules={[
            {
              required: true,
              message: "Enter mpesa number",
            },
          ]}
        >
          <PhoneInput
            required
            name="withDrawPhoneNumber"
            value={formData?.withDrawPhoneNumber}
            onChange={handleNumberChange}
            country="ke"
            onlyCountries={["ke"]}
            countryCodeEditable={false}
            className="input rounded-[6px] border !border-[#cacaca] !h-[42px]"
          />
        </Form.Item>
        <div className="flex justify-between mt-[60px] mb-[48px] items-center">  
        <div className="w-[150px]">
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
              disabled={saving}
               key="submit" type="submit" className="cstm-btn">
              {saving ? <Spin/> : 'Submit'}   
              </button>
            </div> 
        </div>
      </Form>
      </div>
 
    <WithdrawalOtpModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
      />
  </div>
  );
}

export default WithdrawalPage;
