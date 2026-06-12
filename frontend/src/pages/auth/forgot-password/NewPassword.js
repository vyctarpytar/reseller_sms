import React, { useRef, useState } from "react";
import { Form, Input, Spin } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { Link, useNavigate } from "react-router-dom";
import ReactPasswordChecklist from "react-password-checklist";
import svgone from "../../../assets/svg/svgone.svg";
import toast from "react-hot-toast";
import { cleanForgotAcc, cleanSingleUser, save } from "../../../features/auth/authSlice";

export default function NewPassword() {
  const [form] = Form.useForm();
  const formRef = useRef(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const { saving, user, forgotAcc  } = useSelector(
    (state) => state.auth
  );

  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const onFinish = async () => {
    const isPasswordValid =
      password.length >= 8 &&
      /\d/.test(password) &&
      /[A-Z]/.test(password) &&
      /[!@#$%^&*()_+[\]{};':"\\|,.<>/?]+/.test(password) &&
      password === confirmPassword;
    if (!isPasswordValid) {
      toast.error(
        "Password does not meet the required conditions or does not match"
      );
      return;
    }

    const res = await dispatch(
      save({
        usrId: forgotAcc?.usrId,
        usrSecret: password,
        url: "auth/update-password",
      })
    );
    if (res?.payload?.success) {
      toast.success(res.payload?.messages?.message);
      await dispatch(cleanForgotAcc())
      await dispatch(cleanSingleUser());
      await navigate("/password-reset-success");
    } else {
      toast.error(res.payload?.messages?.message);
    }
  };

  return (
    <>
      <div className="w-full min-w-[24rem] h-full flex justify-center items-center">
        <div
          aria-disabled={saving}
          className="flex flex-col items-center mt-[7.12rem]"
        >
          <div className="flex flex-col items-center w-full">
            <h3 className="heading_1">Set new password</h3>
            <span className="zambia_login_text mt-[.75rem]">
              Set a secure password
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
            form={form}
          >
            <Form.Item
              label="Password"
              className="login-form-item"
              name="usrEncryptedPassword"
              rules={[
                {
                  required: true,
                  message: "Please input your password",
                },
              ]}
            >
              <Input.Password
                onChange={(e) => setPassword(e.target.value)}
                className="input-login-password"
                type="password"
              />
            </Form.Item>

            <Form.Item
              label="Confrim password"
              className="login-form-item"
              name="usrEncryptedPasswordAlt"
              rules={[
                {
                  required: true,
                  message: "Please input your password",
                },
              ]}
            >
              <Input.Password
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="input-login-password"
                type="password"
              />
            </Form.Item>

            <span>
              To protect your account, we require that your password has;
            </span>

            <div className="mt-[.32rem]">
              <ReactPasswordChecklist
                className="text-darkBlue"
                rules={[
                  "minLength",
                  "specialChar",
                  "number",
                  "capital",
                  "match",
                ]}
                minLength={8}
                value={password}
                valueAgain={confirmPassword}
                messages={{
                  minLength: "A minimum of 8 characters",
                  number: "At least one number (0-9)",
                  capital: "At least one upper case letter (A-Z)",
                  match: "Confirm password to match with the password",
                  specialChar:
                    "At least one special character e.g !@#$%^&*()_+{}[]:;?/|\\",
                }}
                iconComponents={{
                  ValidIcon: (
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                      strokeWidth="1.5"
                      stroke="currentColor"
                      className="w-6 h-6 text-darkBlue"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                      />
                    </svg>
                  ),
                  InvalidIcon: (
                    <img className="pr-[.5rem]" src={svgone} alt="lazy" />
                  ),
                }}
                validColor="#147CBC"
                iconSize={2}
              />
            </div>

            <button
              disabled={saving}
              className="cstm-btn mt-[2.75rem]"
              type="submit"
            >
              {saving ? <Spin /> : "Next"}
            </button>
          </Form>
        </div>
      </div>
    </>
  );
}
