import React, { useState } from "react";
import { Card, Modal } from "antd";
import group from "../../../../assets/svg/Group.svg"
import upload from "../../../../assets/svg/upload csv.svg";
import { useNavigate } from "react-router-dom";

const HowToAddContactModal = ({ open, handleCancel,uploadGraduateFile }) => {
  const navigate = useNavigate()

  function handleGoToAddGraduates(){
    navigate('/contacts/add')
  }

  function handleUploadGraduateFile(){
    uploadGraduateFile()
  }

 

  return (
    <>
      <Modal
        className="mod mod-height"
        title="Add contact"
        open={open}
        onCancel={handleCancel}
        width={850}
      >
        <div className="grid grid-cols-2 gap-5 p-[41px]">
          <Card className="p-[70px cursor-pointer" onClick={handleGoToAddGraduates}>
            <div className="text-center justify-center flex flex-col ">
              <img
                src={group}
                alt="group"
                className="mx-auto w-[169.002px] h-[161.471px]"
              />

              <span className="text-black text-[20px] font-[500] font-dmSans leading-loose mt-[32px]">
                {" "}
                Complete a Form{" "}
              </span>
            </div>
          </Card>
          <Card className="p-[70px cursor-pointer" onClick={handleUploadGraduateFile}>
            <div className="text-center justify-center flex flex-col ">
              <img
                src={upload}
                alt="upload"
                className="mx-auto w-[169.002px] h-[161.471px]"
              />

              <span className="text-black text-[20px] font-[500] font-dmSans leading-loose mt-[32px]">
                {" "}
                Import excel file{" "}
              </span>
            </div>
          </Card>
        </div>
      </Modal>
    </>
  );
};
export default HowToAddContactModal;
