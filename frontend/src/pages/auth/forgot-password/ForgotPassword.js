import { useDispatch, useSelector } from "react-redux";
import { Link, useNavigate } from "react-router-dom";
import { Form, Input, Spin } from "antd";
import { useRef } from "react";
import toast from "react-hot-toast";
import { resetPassword } from "../../../features/auth/authSlice";


export default function ForgotPassword() {
  const [form] = Form.useForm();
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const formRef = useRef(null);

  const { authLoading, saving } = useSelector((state) => state.auth);

  const onFinish = async (data) => {
    const res = await dispatch(
      resetPassword({ 
        email: data.usrEmail,
        url: `auth/forgot-password`,
      })
    );
 
    if (res?.payload?.success) {
      toast.success(res.payload?.messages?.message);
      await navigate("/verify-forgot-email");
    } else {
      toast.error(res.payload?.messages?.message);
    }
  };

  return (
    <>


      <div className="w-full min-w-[24rem] h-full flex justify-center items-center">
        <div
          aria-disabled={authLoading}
          className="flex flex-col items-center mt-[7.12rem]"
        >
          <div className="flex flex-col items-center w-full">
            <h3 className="heading_1">Forgot password</h3>
            <span className="zambia_login_text mt-[.75rem]">
              Let’s reset your password
            </span>
          </div>
          <Form
            layout="vertical"
            ref={formRef}
            name="control-ref"
            onFinish={onFinish}
            style={{
              maxWidth: "100%",
              width: "24rem",
              marginTop: "3.75rem",
            }}
            className="lg:px-0 px-3"
            form={form}
          >
            <Form.Item
              label="Email address*"
              className="login-form-item"
              name="usrEmail"
              rules={[
                {
                  required: true,
                  message: "Please input your email",
                },
              ]}
            >
              <Input className="input-login" />
            </Form.Item>

            <button
              disabled={saving}
              className="cstm-btn mt-[2.75rem]"
              type="submit"
            >
              {saving ? <Spin /> : "Next"}
            </button>

            <div className="mt-[2.25rem] flex justify-center items-center">
              <span className="dnt_have_account_text">
                Don’t have an account? 
              </span>
              <Link
                className="forgot_text ml-[.5rem]"
                to="/login"
              >
               Login
              </Link>
            </div>
          </Form>
        </div>
      </div>
    </>
  );
}
