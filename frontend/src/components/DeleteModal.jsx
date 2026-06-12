import React, { useEffect, useRef, useState } from "react";
import { Form, Input, Modal, Button, Switch,Spin } from "antd"; 
import { useDispatch, useSelector } from "react-redux";  
import toast from "react-hot-toast"

const DeleteModal = ({
  isModalOpen,
  setIsModalOpen,
  content, 
  handleDelete,
  loading,
  title
}) => {
  const dispatch =  useDispatch();
 
  const handleOk = () => {
    setIsModalOpen(false);
  };
  const handleCancel = () => {
    setIsModalOpen(false); 
  };
 
  const { user } = useSelector((state) => state.auth);
 
  

  return (
    <>
      <Modal
        className="modal-btn mod-height mod-footer activity activity-header activity-close"
        title={title ? title : "Delete Request"}
        open={isModalOpen}
        onCancel={handleCancel}
        width={898}
      >
        <div className="flex justify-center items-center mt-[51px]">
          <div className="w-[80px] h-[80px] rounded-full bg-[#FFF6F6] flex items-center justify-center">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="34"
              height="34"
              viewBox="0 0 34 34"
              fill="none"
            >
              <path
                d="M4.25 8.5026H7.08333M7.08333 8.5026H29.75M7.08333 8.5026V28.3359C7.08333 29.0874 7.38184 29.8081 7.9132 30.3394C8.44455 30.8708 9.16522 31.1693 9.91667 31.1693H24.0833C24.8348 31.1693 25.5554 30.8708 26.0868 30.3394C26.6182 29.8081 26.9167 29.0874 26.9167 28.3359V8.5026H7.08333ZM11.3333 8.5026V5.66927C11.3333 4.91782 11.6318 4.19715 12.1632 3.6658C12.6946 3.13445 13.4152 2.83594 14.1667 2.83594H19.8333C20.5848 2.83594 21.3054 3.13445 21.8368 3.6658C22.3682 4.19715 22.6667 4.91782 22.6667 5.66927V8.5026M14.1667 15.5859V24.0859M19.8333 15.5859V24.0859"
                stroke="#B42318"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </div>
        </div>

        <div className="mt-[38px] text-center text-black1 text-[21px] font-normal font-dmSans leading-loose">
         {content}
        </div>

        <div className="flex flex-col items-center mt-[63px] gap-y-5 mb-[66px]">
          <Button
            className="text-white text-xl font-medium font-dmSansleading-normal bg-darkGreen
          w-[239px] h-[46px] px-[18px] py-3  rounded-[56px] justify-center items-center gap-2.5 inline-flex"
            type="submit"
            onClick={handleDelete}
            disabled={loading}
          >
            {
              loading ? <Spin/> : 'Continue'
            }
            
          </Button>

          <button
            className="text-darkGreen text-xl font-medium font-dmSansleading-normal
          w-[239px] h-[46px] px-[18px] py-3  rounded-[56px] border border-darkGreen justify-center items-center gap-2.5 inline-flex"
            onClick={handleCancel}
          >
            Close
          </button>
        </div>
      </Modal>
    </>
  );
};
export default DeleteModal;