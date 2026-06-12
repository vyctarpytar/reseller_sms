import React, { useEffect, useRef, useState } from "react";
import { Form, Input, Modal, Button, Switch, Spin } from "antd";
import { useDispatch, useSelector } from "react-redux";
import toast from "react-hot-toast";
import MaterialIcon from "material-icons-react";

const ConfirmationModal = ({
  isModalOpen,
  setIsModalOpen,
  handleBack,
  content,
  subContent,
}) => {
  const handleOk = () => {
    setIsModalOpen(false);
  };
  const handleCancel = () => {
    setIsModalOpen(false);
  };
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const { user } = useSelector((state) => state.auth);

  return (
    <>
      <Modal
        className=""
        title="Confirm Submission"
        open={isModalOpen}
        onCancel={handleCancel}
        width={578}
      >
        <div className="flex justify-center text-center items-center">
          <div className="mt-[0px] flex items-center justify-center text-center rounded-full h-[80px] w-[80px] border-[4px] border-[#f8bb86]">
            <button>
              <MaterialIcon color="#f8bb86" icon="priority_high" />
            </button>
          </div>
        </div>

        <div className="mt-[18px] text-center justify-center text-black1 font-dmSans text-[27px] font-medium  leading-[24px]">
          {content}
        </div>
        {subContent && (
          <div className="mt-[13px]  justify-start text-[#000000A3] font-dmSans text-[18px] font-[400]  leading-[24px]">
            {subContent}
          </div>
        )}

        <div className="flex justify-end items-center mt-[100px] gap-x-2 mb-[66px]">
          <div className="w-[150px]">
            <button
              className="cstm-btn !bg-[#EFEFEF] !text-[#555555]"
              onClick={handleCancel}
            >
              No,stay
            </button>
          </div>

          <div className="w-[150px]">
            <button className="cstm-btn " onClick={handleBack}>
              Yes,leave
            </button>
          </div>
        </div>
      </Modal>
    </>
  );
};
export default ConfirmationModal;
