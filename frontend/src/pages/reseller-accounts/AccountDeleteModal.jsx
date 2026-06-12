import React, { useEffect, useRef } from "react";
import { Form, Input, Modal, Spin } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import svg50 from "../../assets/svg/svg50.svg";
import { deleteRequest, save } from "../../features/save/saveSlice";

const { TextArea } = Input;
const AccountDeleteModal = ({ open, handleCancel, prodd, handleFetch }) => {
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const { user } = useSelector((state) => state.auth);
  const { saving } = useSelector((state) => state.save);

  const dispatch = useDispatch();
  const navigate = useNavigate();
 
  const onFinish = async (data) => {
    const res = await dispatch(
      deleteRequest({
        url: `api/v2/account/${prodd?.accId}`,
        ...data
      })
    );
    if (res?.payload?.success) {
      toast.success(res?.payload?.messages?.message);
      await handleFetch();
      await handleCancel();
      await form.resetFields()
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  return (
    <>
      <Modal
        className=""
        title={`Delete Account `}
        open={open}
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
          <div className="flex items-center gap-3 mb-[1.25rem]">
            <img src={svg50} alt="svg50" />
            <span className="details_h">
              This will result to deletion of customer data in 30days
            </span>
          </div>

          <Form.Item
            name="acDeleteReason"
            label={
              <span>
                Reason<span className="text-[#FF0000]">*</span>
              </span>
            }
            rules={[
              {
                required: true,
                message: "Please add sms unit price",
              },
            ]}
          >
            <TextArea rows={4} className="input-textarea" />
          </Form.Item>

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
                  {saving ? <Spin /> : "Submit"}
                </button>
              </div>
            </div>
          </div>
        </Form>
      </Modal>
    </>
  );
};
export default AccountDeleteModal;
