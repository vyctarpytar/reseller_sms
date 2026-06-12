import React from "react";
import { Modal, Spin } from "antd";
import svg49 from "../assets/svg/svg49.svg";
import svg50 from "../assets/svg/svg50.svg";

const ConfirmModal = ({
  open,
  handleCancel,
  handleSubmit,
  loading,
  content,
  type,
  btnTitle,
}) => {
  return (
    <>
      <Modal
        className="!w-[31.6875rem]"
        title="Confirm Action"
        open={open}
        onOk={handleSubmit}
        onCancel={handleCancel}
      >
        <div className="my-[2.69rem] flex items-center justify-center gap-x-[2.69rem]">
          {type == "alert" ? (
            <img src={svg50} alt="svg50" />
          ) : (
            <img src={svg49} alt="svg49" />
          )}

          <span className="details_h">{content}</span>
        </div>

        <div className="flex justify-end items-center  gap-x-[.75rem]">
          <div className="w-[100px]">
            <button className="cstm-btn-2 " onClick={handleCancel}>
              Cancel
            </button>
          </div>

          <div className="w-[150px]">
            <button
              disabled={loading}
              className={`cstm-btn ${
                type === "alert" ? "!bg-[#B42318]" : "inherit"
              }`}
              onClick={handleSubmit}
            >
              {loading ? <Spin /> : btnTitle}
            </button>
          </div>
        </div>
      </Modal>
    </>
  );
};
export default ConfirmModal;
