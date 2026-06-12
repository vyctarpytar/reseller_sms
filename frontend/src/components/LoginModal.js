import React, { useRef } from "react";
import { Form, Input, Modal, Spin } from "antd";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { login, setToken } from "../features/auth/authSlice";
import { customToast } from "../utils";
import { fetchMenu } from "../features/menu/menuSlice";
import toast from "react-hot-toast";
import axiosInstance from "../instance";

export default function LoginModal({ open, handleCancel }) {
  const [form] = Form.useForm();
  const formRef = useRef(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const { authLoading } = useSelector((state) => state.auth);

  const onFinish = async (data) => {
    const res = await dispatch(login(data));
    if (res?.payload?.success) {
      await dispatch(setToken(res?.payload?.access_token));
      axiosInstance.defaults.headers.common["Authorization"] =
        await `Bearer ${res?.payload?.access_token}`;
      toast.success("Successfully logged in");
      // await dispatch(fetchMenu());
      await navigate("/dashboard");
    } else {
      toast.error("Error logging in, kindly try again");
    }
  };
  return (
    <>
      <Modal
        className=""
        title="Login to continue"
        open={open}
        onCancel={handleCancel}
        width={850}
      >
        <Form
          layout="vertical"
          ref={formRef}
          name="control-ref"
          onFinish={onFinish}
          style={{
            maxWidth: "100%",
            width: "700px",
          }}
          form={form}
        >
          <h1>Your session has ended</h1>
          <h5 className="mb-5">Log in to continue </h5>

          <Form.Item
            label="Email address"
            className="login-form-item"
            name="email"
            rules={[
              {
                required: true,
                message: "Please input your email",
              },
            ]}
          >
            <Input type="email" className="input-login" />
          </Form.Item>
          <Form.Item
            label="Password"
            className="login-form-item"
            name="password"
            rules={[
              {
                required: true,
                message: "Please input your password",
              },
            ]}
          >
            <Input.Password className="input-login" type="password" />
          </Form.Item>

          <button
            disabled={authLoading}
            className="cstm-btn my-5"
            type="submit"
          >
            {authLoading ? <Spin /> : "Login"}
          </button>
        </Form>
      </Modal>
    </>
  );
}
