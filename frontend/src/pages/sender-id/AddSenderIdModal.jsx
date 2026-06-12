import React, { useEffect, useRef, useState } from "react";
import { 
  Form, 
  Input, 
  Modal,
  Spin,
} from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom"; 
import toast from "react-hot-toast"; 
import {   save } from "../../features/save/saveSlice"; 
 
const AddSenderIdModal = ({ isModalOpen, setIsModalOpen, prodd }) => {
 
  const handleCancel = () => {
    setIsModalOpen(false);
  };
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const { user } = useSelector(
    (state) => state.auth
  );
  const { saving } = useSelector((state) => state.save);
  

  const dispatch = useDispatch();
  const navigate = useNavigate();

  function closeModal() {
    toast.dismiss();
  }

  
  const onFinish = async (data) => { 
    const res = await dispatch(
      save({
        url: `api/v2/shortcode/register`,
        ...data
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message); 
      await form.resetFields();
      await setIsModalOpen(false);
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  
  // useEffect(() => {
  //   if (prodd?.senderId) {   
  //     form.setFieldsValue(prodd);
  //   }else{ 
  //     form.resetFields("");
  //   }
  // }, [prodd,form]);
  
 

  return (
    <>
      <Modal
        className=""
        title={`Add  Sender ID`}
        open={isModalOpen}
        onOk={onFinish}
        onCancel={handleCancel}
        width={800}
      >
        <div className="lg:px-5 px-3">
        
          <Form
            layout="vertical"
            ref={formRef}
            name="control-ref"
            onFinish={onFinish}
            className=""
            style={{
              maxWidth: "100%",
            }}
            form={form} 
          >
            <Form.Item 
              label="Sender ID"
              name="shCode"
              rules={[
                {
                  required: true,
                  message: "Required field",
                },
              ]}
              className="w-full"
            >
             <Input className="input"/>
            </Form.Item>

            <div className="flex justify-between mt-[48px] ">
              <div className="justify-start"></div>
              <div className="justify-end flex items-center mb-[58px] gap-x-5">
                <div className="w-[150px] ">
                  <button
                    type="button"
                    key="back"
                    onClick={handleCancel}
                    className="cstm-btn !bg-white !text-[#388E3C] !border !border-[#388E3C]"
                  >
                    Cancel
                  </button>
                </div>

                <div className="w-[200px]">
                  <button key="submit" type="submit" className="cstm-btn">
                    {saving ? <Spin /> : "Create"}
                  </button>
                </div>
              </div>
            </div>
          </Form>
        </div>
      </Modal>
    </>
  );
};
export default AddSenderIdModal;
