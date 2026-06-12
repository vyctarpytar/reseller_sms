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

  const isFuturesoft = subdomain === "futuresoft";

  return (
    <div className="flex h-[100vh] w-full bg-white">
      {/* ── Brand / imagery side ── */}
      <div
        className={`w-[50%] h-full lg:flex hidden justify-center items-center relative
        ${
          subdomain == "synqafrica"
            ? "login-pic-sync"
            : subdomain == "synqtel"
            ? "login-pic-synctel"
            : subdomain == "futuresoft"
            ? "login-pic-futuresoft"
            : "login-pic-sync"
        }`}
      >
        {/* subtle brand gradient veil for depth + legibility */}
        <div
          className="absolute inset-0"
          style={{
            background:
              "linear-gradient(160deg, rgba(105,71,46,0.20) 0%, rgba(19,22,29,0.55) 100%)",
          }}
        />
      </div>

      {/* ── Form side ── */}
      <div className="lg:w-[50%] w-full flex flex-col justify-center items-center lg:px-[10%] px-5 bg-surface">
        <div className="w-full max-w-[420px]">
          <div className="image-container !justify-start mb-8">
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
                  : syncLogo
              }
              alt="logo"
              className="h-[120px] lg:h-[140px] w-auto object-contain animated-image"
            />
          </div>

          <p className="sa-eyebrow">Welcome back</p>
          <h1 className="text-[28px] font-semibold leading-tight text-primary mb-1">
            Sign in to your account
          </h1>
          <p className="text-sm text-muted mb-8">
            Enter your credentials to access the dashboard.
          </p>

          <Form
            layout="vertical"
            ref={formRef}
            name="control-ref"
            onFinish={onFinish}
            style={{ maxWidth: "100%" }}
            className="w-full"
            form={form}
            requiredMark={false}
          >
            <Form.Item
              label={<span className="text-sm font-medium text-ink">Email address</span>}
              className="login-form-item"
              name="email"
              rules={[{ required: true, message: "Please input your email" }]}
            >
              <Input type="email" size="large" placeholder="you@company.com" />
            </Form.Item>
            <Form.Item
              label={<span className="text-sm font-medium text-ink">Password</span>}
              className="login-form-item !mb-2"
              name="password"
              rules={[{ required: true, message: "Please input your password" }]}
            >
              <Input.Password size="large" type="password" placeholder="••••••••" />
            </Form.Item>

            <div className="w-full flex justify-end mb-6">
              <Link
                className="text-sm font-medium hover:underline"
                style={{ color: isFuturesoft ? "#1B47B4" : "#69472E" }}
                to="/forgot-password"
              >
                Forgot password?
              </Link>
            </div>

            <button
              disabled={authLoading}
              className="w-full h-[48px] rounded-lg text-white text-sm font-semibold transition-all duration-200 hover:-translate-y-0.5 disabled:opacity-60 disabled:translate-y-0 flex items-center justify-center"
              style={{
                background: isFuturesoft ? "#147CBC" : "#69472E",
                boxShadow: "0 10px 15px -3px rgba(105,71,46,0.25)",
              }}
              type="submit"
            >
              {authLoading ? <Spin size="small" /> : "Sign in"}
            </button>

            <p className="text-center text-xs text-muted mt-8">
              Powered by Synq Africa
            </p>
          </Form>
        </div>
      </div>
    </div>
  );
}

export default Login;
