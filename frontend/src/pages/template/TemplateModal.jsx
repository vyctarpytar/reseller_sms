import React, { useEffect, useRef, useState } from "react";
import { DatePicker, Form, Input, Modal, Select, Spin } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import MessageTemplate from "../mesage-template/MessageTemplate";
import { fetchTemplates, save } from "../../features/save/saveSlice";
 

const { TextArea } = Input;
const TemplateModal = ({
  isModalOpen,
  setIsModalOpen,
  prodd,
  title,
  formData,
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
  const { saving } = useSelector((state) => state.save);

  const linkRef = useRef(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [data, setdata] = useState({});
  const [inputValue, setInputValue] = useState(prodd? prodd?.tmpMessage :"");


  async function fetchTemplatesData(){
    await dispatch(fetchTemplates({
      url:'api/v2/msgTemp/list'
    }))
  }

  const onFinish = async (data) => {
    const res = await dispatch(
      save({
        url: prodd ? `api/v2/msgTemp/update/${prodd?.tmpId}` : `api/v2/msgTemp/save`,
        tmpMessage:inputValue,
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await fetchTemplatesData();
      form.resetFields();
      await setIsModalOpen(false);
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  useEffect(()=>{
    setInputValue(prodd? prodd?.tmpMessage :"")
  },[prodd])

  return (
    <>
      <Modal
        className=""
        title={prodd ? "Update template" : title }
        open={isModalOpen}
        onOk={onFinish}
        onCancel={handleCancel}
        width={900}
      >
        <Form
          layout="vertical"
          ref={formRef}
          name="control-ref"
          onFinish={onFinish}
          className="px-[15px]"
          style={{
            maxWidth: "100%",
          }}
          form={form}
        >
          <MessageTemplate
            inputValue={inputValue}
            setInputValue={setInputValue}
          />

          <div className="flex justify-between mt-[48px] ">
            <div className="justify-start"></div>
            <div className="justify-end flex items-center mb-[58px] gap-x-5">
              <div className="w-[150px] ">
                <button
                  key="back"
                  type="button"
                  onClick={handleCancel}
                  className="cstm-btn !bg-white !text-[#388E3C] !border !border-[#388E3C]"
                >
                  Cancel
                </button>
              </div>

              <div className="w-[150px]">
                <button
                  key="submit"
                  type="submit"
                  className="cstm-btn"
                  disabled={saving}
                >
                  {saving ? <Spin /> : "Create"}
                </button>
              </div>
            </div>
          </div>
        </Form>
      </Modal>
    </>
  );
};
export default TemplateModal;
