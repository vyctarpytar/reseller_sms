import { Spin } from "antd";
import React from "react";

export default function TableLoading() {
  return (
    <div
      style={{
        border: "1px solid rgba(224, 224, 224, 0.00)",
        background: "var(--Gray-100, #F2F4F7)",
      }}
      className="w-full mt-[1.5rem] h-[13.75rem] rounded-[.5rem] flex justify-center items-center"
    >
      <Spin className="sms-spin" size="large" />
    </div>
  );
}
