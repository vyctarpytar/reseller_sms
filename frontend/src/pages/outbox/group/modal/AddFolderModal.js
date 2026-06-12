import React, { useEffect, useRef, useState } from "react";
import { Button, Form, Input, Modal, Select, Spin, Tag } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import {
  saveFolder,
  setFolderObj,
} from "../../../../features/folder/folderSlice";
import { save } from "../../../../features/save/saveSlice";
import toast from "react-hot-toast";

const { TextArea } = Input;

const AddFolderModal = ({ open, handleCancel, handleFetchData, prodd }) => {
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const { fldLoading } = useSelector((state) => state.folder);
  const { user } = useSelector((state) => state.auth);
  const { saving } = useSelector((state) => state.save);
  const [data, setData] = useState({});

 

  const [formData, setFormData] = useState(prodd);

  const onChange = async (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  const onFinish = async (data) => {
    const res = await dispatch(
      save({
        groupId:prodd?.groupId ? prodd?.groupId : null,
        groupName: formData?.groupName,
        groupDescription: formData?.groupDescription,
        url: "api/v2/groups",
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await handleFetchData();
      await handleCancel();
      await form.resetFields();
      await dispatch(setFolderObj(res?.payload?.data?.result));
      await navigate(`/contacts/folder/${formData?.groupName}`);
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  useEffect(() => {
    setFormData(prodd);
  }, [prodd]);

  return (
    <>
      <Modal
        className="modal-btn mod-height mod-footer"
        title="Add contact group"
        open={open}
        onOk={onFinish}
        onCancel={handleCancel}
        width={850}
      >
        <Form
          layout="vertical"
          ref={formRef}
          name="control-ref"
          onFinish={onFinish}
          className=" "
          style={{
            maxWidth: "100%",
          }}
          form={form}
        >
          <Form.Item
            label="Group name (e.g June 2024)"
            rules={[
              {
                required: true,
                message: "Enter name of group",
              },
            ]}
          >
            <Input
              required
              name="groupName"
              onChange={onChange}
              value={formData?.groupName}
              className="input"
            />
          </Form.Item>
          <div className="w-full">
            <Form.Item
              label="Description"
              rules={[
                {
                  required: true,
                  message: "Enter description of this group",
                },
              ]}
              extra={"Write a brief description of this group"}
            >
              <TextArea
                required
                name="groupDescription"
                onChange={onChange}
                value={formData?.groupDescription}
                rows={4}
                placeholder="Enter description"
                className="input-textarea"
              />
            </Form.Item>
          </div>
          <div className="flex justify-between mt-[156px] mb-[48px]">
            <div className="justify-start"></div>
            <div className="justify-end flex items-center gap-x-2">
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
export default AddFolderModal;
