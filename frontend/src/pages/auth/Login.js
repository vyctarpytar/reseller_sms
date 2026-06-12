import React, { useEffect, useRef, useState } from "react";
import { Form, Input, Spin } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { Link, useNavigate } from "react-router-dom";
import logo from "../../assets/img/spa_logo.png";
import {
  cleanAuthLoading,
  clearAuthObj,
  login,
  setIsLoggedIn,
  setToken,
} from "../../features/auth/authSlice";
import axiosInstance from "../../instance";
import toast from "react-hot-toast";
import { fetchMenu } from "../../features/menu/menuSlice";
import weiser from "../../assets/img/weiser-logo.png";
import sideImage from "../../assets/img/sideImage.jpg";
import { getSubdomain } from "../../utils";
import syncLogo from "../../assets/img/sync-logo.png";
import synctelLogo from "../../assets/img/synqtel-logo-login.png";
import futuresoftLogo from "../../assets/img/futuresoft-logo-login.png";
import "./login.css";
import { cleanLegendClickStatus } from "../../features/global/globalSlice";

function Login() {
  const [form] = Form.useForm();
  const formRef = useRef(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const { authLoading, user } = useSelector((state) => state.auth);

  const onFinish = async (data) => {
    const res = await dispatch(login(data));
    if (res?.payload?.success) {
      await dispatch(setToken(res?.payload?.access_token));
      axiosInstance.defaults.headers.common["Authorization"] =
        await `Bearer ${res?.payload?.access_token}`;
      toast.success("Successfully logged in");
      await dispatch(cleanLegendClickStatus());
      await dispatch(fetchMenu());
      await navigate("/dashboard-main");
    } else {
      toast.error(res?.payload?.messages?.message ?? "Bad Credentials");
    }
  };

  const [subdomain, setSubdomain] = useState("");

  useEffect(() => {
    setSubdomain(getSubdomain());
  }, []);

  async function clean() {
    await dispatch(clearAuthObj());
  }

  useEffect(() => {
    clean();
  }, []);

  useEffect(() => {
    dispatch(cleanAuthLoading());
    localStorage.clear();
  }, []);

  return (
    <div className="flex h-[100vh] w-full">
      <div
        className={`w-[50%] h-full lg:flex hidden justify-center items-center
        ${
          subdomain == "synqafrica"
            ? "login-pic-sync"
            : subdomain == "synqtel"
            ? "login-pic-synctel"
            : subdomain == "futuresoft"
            ? "login-pic-futuresoft"
            : "login-pic"
        }`}
      ></div>
      <div className="bg-[#d9d3d3] lg:w-[50%] w-full flex flex-col  items-center lg:px-[10%] px-3">
        <div className="image-container">
          <img
            loading="lazy"
            decoding="async"
            src={
              subdomain == "synqafrica"
                ? syncLogo
                : subdomain == "synqtel"
                ? synctelLogo
                : subdomain == "futuresoft"
                ? futuresoftLogo
                : weiser
            }
            alt="logo"
            className="w-[450px] animated-image"
          />
        </div>
        <Form
          layout="vertical"
          ref={formRef}
          name="control-ref"
          onFinish={onFinish}
          style={{
            maxWidth: "100%",
          }}
          className="w-full mt-1"
          form={form}
        >
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
            className={`cstm-btn ${
              subdomain == "synqafrica" || subdomain == "synqtel"
                ? "!bg-syncBtn"
                : "!bg-darkBlue"
            }   mt-[3.25rem]`}
            type="submit"
          >
            {authLoading ? <Spin /> : "Login"}
          </button>

          <div className="w-full flex lg:flex-row flex-col justify-start lg:items-center items-start mt-[1.75rem]">
            <Link
              className={`${
                subdomain == "synqafrica" || subdomain == "synqtel"
                  ? "!text-syncBtn"
                  : "text-[#1B47B4]"
              } forgot_text lg:px-0 px-0`}
              to="/forgot-password"
            >
              Forgot Password ?
            </Link>
          </div>

          {/* <div className="mt-[6.19rem] flex justify-center items-center">
            <span className="dnt_have_account_text !text-[#0057E3]">
              Powered By Smart People Africa
            </span>
          </div> */}
        </Form>
      </div>
    </div>
  );
}

export default Login;
