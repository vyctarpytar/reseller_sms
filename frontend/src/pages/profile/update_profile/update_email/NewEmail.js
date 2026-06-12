import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { useEffect, useRef } from "react";
import { Form, Input, Spin } from "antd";
import {  saveForget } from "../../../../features/save/saveSlice";
import toast from "react-hot-toast";

export default function NewEmail() {
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [form] = Form.useForm();
  const formRef = useRef(null);

  const { user, authLoading } = useSelector((state) => state.auth);

  const { saving } = useSelector((state) => state.save);

  const handleBack = () => {
    navigate("/account-settings");
  };

  const onFinish = async (data) => {
    const res = await dispatch(
		saveForget({
        url: `api/v1/usr/new-email-otp/${user?.drftId}?newEmail=${data?.usrEmail}`,
      })
    );
    if (res?.payload?.success) {
      toast.success(res.payload?.messages?.message);
      await navigate("/account-settings/verify-new-email");
      form.resetFields();
    } else {
      toast.error(res.payload?.messages?.message);
    } 
  };

  useEffect(() => {}, [user]);

  return (
    <>
      <div className="w-full h-full flex justify-center items-center">
        <div aria-disabled={authLoading} className="flex flex-col items-center">
          <div className="flex flex-col items-center w-full">
            <h3 className="heading_1">New Email</h3>
          </div>

          <Form
            layout="vertical"
            ref={formRef}
            name="control-ref"
            onFinish={onFinish}
            style={{
              maxWidth: "100%",
            }}
            className="mt-[68px]"
            form={form}
          >
            <div className="grid grid-cols-1">
              <Form.Item
                name="usrEmail"
                label="Enter your new email *"
                rules={[
                  {
                    required: true,
                    message: "Add new phone number",
                  },
                  {
                    type: "email",
                    message: "Enter a valid email",
                  },
                ]}
              >
                <Input className="input-login" />
              </Form.Item>
            </div>
            <div className="mt-[2rem] flex items-center justify-center gap-x-3">
              <div className="w-[185px]">
                <button
                  className={`cstm-btn !bg-[#fff] !text-[#374151] border border-[#D1D5DB]`}
                  onClick={handleBack}
                >
                  Back
                </button>
              </div>

              <div className="w-[185px]">
                <button type="submit" key="submit" className={`cstm-btn`}>
                  {saving ? <Spin /> : "Next"}
                </button>
              </div>
            </div>
          </Form>
        </div>
      </div>
    </>
  );
}
