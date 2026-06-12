import React, { useEffect, useRef, useState } from "react";
import { Button, Form, Input, Spin } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom"; 
import toast from "react-hot-toast";
import { obfuscateEmail } from "../../../utils";
import { save, saveOtp } from "../../../features/auth/authSlice";
 

function VerifyForgotEmail() {
  const [form] = Form.useForm();
  const formRef = useRef(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({});
  const [isInputEmpty, setIsInputEmpty] = useState(true);

  const [counter, setCounter] = useState({ minutes: 0, seconds: 59 });
  const [activeResend, setActiveResend] = useState(false);

  const { accType, forgotAcc, saving } = useSelector((state) => state.auth);
  const resendOTPEmail = async () => {
    const res = await dispatch(
      saveOtp({
        url: `auth/send-email-otp/${forgotAcc?.usrId}`,
      })
    );
    if (res?.payload?.success) {
      toast.success(res.payload?.messages?.message);
    } else {
      toast.error(res.payload?.messages?.message);
    }
  };

  const handleBack = async () => {
    await navigate("/forgot-password");
  };
 
  const onFinish = async (data) => {
    const res = await dispatch(
      save({
        usrId: forgotAcc?.usrId,
        usrSecret: data?.usrSecret,
        url: "auth/validate-email-otp",
      })
    );
    if (res?.payload?.success) {
      toast.success(res.payload?.messages?.message);
      await navigate("/new-password");
      setIsInputEmpty(true);
      form.resetFields();
    } else {
      toast.error(res.payload?.messages?.message);
    }
  };

  const handleInputChange = (e) => {
    const value = e.target.value;
    setIsInputEmpty(!value);
    setFormData((prevData) => ({
      ...prevData,
      usrFirstName: value,
    }));
  };

  useEffect(() => {
    const interval = setInterval(() => {
      if (counter.minutes === 0 && counter.seconds === 0) {
        clearInterval(interval);
      } else {
        setCounter((prevCounter) => {
          if (prevCounter.seconds === 0) {
            return { minutes: prevCounter.minutes - 1, seconds: 59 };
          } else {
            return { ...prevCounter, seconds: prevCounter.seconds - 1 };
          }
        });
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [counter]);

  return (
    <>
 
      <div className="w-full h-full flex justify-center items-center">
        <div className="flex flex-col mt-[56px] w-[450px]">
          <div className="mt-[56px] heading_1">Verify your email</div>
          <div className="heading_5">We have sent an OTP to your email</div>

          <Form
            layout="vertical"
            ref={formRef}
            name="control-ref"
            onFinish={onFinish}
            style={{
              maxWidth: "100%",
            }}
            className="mt-[78px] lg:px-0 px-3"
            form={form}
          >
            <div className="grid grid-cols-1">
              <Form.Item
                name="usrSecret"
                label={
                  <span>
                    Enter OTP sent to{" "}
                    {forgotAcc?.email
                      ? obfuscateEmail(forgotAcc?.email)
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
                <Input onChange={handleInputChange} className="input-login" />
              </Form.Item>
            </div>
            <div className="mt-[22px] flex items-center justify-center gap-x-3">
              <div className="w-[185px]">
                <button
                  className={`cstm-btn !bg-[#fff] !text-[#374151] border border-[#D1D5DB]`}
                  onClick={handleBack}
                >
                  Back
                </button>
              </div>

              <div className="w-[185px]">
                <button
                  type="submit"
                  key="submit"
                  className={`cstm-btn ${
                    isInputEmpty ? "!bg-[#D1D5DB]" : "inherit"
                  } ${isInputEmpty ? "cursor-not-allowed" : "inherit"}`}
                  disabled={isInputEmpty}
                >
                  {saving ? <Spin /> : "Next"}
                </button>
              </div>
            </div>

            <div className="mt-[66px] flex items-center justify-center typography_p_2 gap-x-1">
              Didn’t receive OTP?
              {counter.minutes === 0 && counter.seconds === 0 ? (
                <span
                  className="font-[600] typography_p_2 !text-[#0057E3] cursor-pointer"
                  onClick={resendOTPEmail}
                  disabled={counter.minutes > 0 && counter.seconds > 0}
                  type="primary"
                >
                  Resend
                </span>
              ) : (
                <span className="font-[600] typography_p_2">
                  {" "}
                  Resend in {counter.minutes.toString().padStart(2, "0")}:
                  {counter.seconds.toString().padStart(2, "0")}
                </span>
              )}
            </div>
          </Form>
        </div>
      </div>
    </>
  );
}

export default VerifyForgotEmail;
