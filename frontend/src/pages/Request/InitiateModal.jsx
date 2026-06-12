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
import { save } from "../../features/save/saveSlice"; 
import { fetchReseller } from "../../features/reseller/resellerSlice";

 
const InitiateModal = ({ isModalOpen, setIsModalOpen, id }) => {
 
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

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [data, setdata] = useState({});
  const [shChannel, setShChannel] = useState('KENYA.SAFARICOM');  

 
  const handleInputChange = (e) => {
    setShChannel(e.target.value);
  };

  function fetchResellerData() {
    dispatch(fetchReseller());
  }
 

  const onFinish = async (data) => {
    if(!shChannel){
      toast.error("Provide channel");
      return
    }
    const res = await dispatch(
      save({
        url: `api/v2/setup/initiate/${id}`,
        shId: null,
        shCode: data?.shCode,  
        shCampaignId: data?.shCampaignId, 
        shChannel: shChannel, 
        shAccId: "",
        shPrsp: "WEISER", 
        // shResellerId:'',
      })
    );
    if (res?.payload?.success) {
      toast.success(res?.payload?.messages?.message);
      navigate("/product-request-list");
      form.resetFields();
      setIsModalOpen(false);
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };
  return (
    <>
      <Modal
        className=""
        title="Add Codes"
        open={isModalOpen}
        onOk={onFinish}
        onCancel={handleCancel}
        width={850}
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
          <div className="grid lg:grid-cols-2 grid-cols-1 lg:gap-x-5 lg:gap-y-0 gap-y-5">
            <Form.Item
              name="shCode"
              label={
                <span>
                  Code<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add code",
                },
              ]}
            >
              <Input className="input" />
            </Form.Item>
              
            <Form.Item
              name="shCampaignId"
              label={
                <span>
                  Campaign ID 
                </span>
              }
              rules={[
                {
                  required: false,
                  message: "Please add Campaign id",
                },
              ]}
            >
              <Input className="input" />
            </Form.Item>

           

            <Form.Item
        name="shChannel"
        label={
          <span>
            Channel<span className="text-[#FF0000]">*</span>
          </span>
        }
        initialValue={shChannel}  
        rules={[
          {
            required: false,
            message: "Please add channel",
          },
        ]}
      >
        <Input
          value={shChannel}  
          onChange={handleInputChange}  
          className="input"
        />
      </Form.Item>

            <Form.Item 
              label={
                <span>
                  Prsp 
                </span>
              }  
            >
              <Input  name="shPrsp" value={"WEISER"} className="input" />
            </Form.Item>
 
          </div>

          <div className="flex justify-between mt-[48px] ">
            <div className="justify-start"></div>
            <div className="justify-end flex items-center mb-[58px] gap-x-5">
              <button
                key="back"
                type="button"
                onClick={handleCancel}
                className="cstm-btn !bg-white !text-[#388E3C] !border !border-[#388E3C]"
              >
                Cancel
              </button>

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
export default InitiateModal;
