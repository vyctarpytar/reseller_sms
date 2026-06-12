import React, { useEffect, useRef, useState } from "react";
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Spin,
  Select,
  InputNumber,
  Space,
  Divider,
  Checkbox,
} from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import MaterialIcon from "material-icons-react";
import uplodSvg from "../../assets/img/uploadPic.png";
import toast from "react-hot-toast";
import { formatImgPath, getLetterWord } from "../../utils";
import { accessLevelData, statusData } from "../../data";
import { save } from "../../features/save/saveSlice";
import { fetchMyUsers, fetchPermissions } from "../../features/auth/authSlice";
import PhoneInput from "react-phone-input-2";

const roleOptions = [
  {
    label: "Recruiter",
    value: 1,
  },
];
const UserPermissionModal = ({ isModalOpen, setIsModalOpen, prodd }) => {
  const handleOk = () => {
    setIsModalOpen(false);
  };
  const handleCancel = () => {
    setIsModalOpen(false);
  };
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const { user, usersLoading, permissionData } = useSelector(
    (state) => state.auth
  );
  const { saving } = useSelector((state) => state.save);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  function closeModal() {
    toast.dismiss();
  }

  const [formData, setFormData] = useState({ ...prodd });
  const [data, setdata] = useState({});

  const [permissionValue, setPermissionValue] = useState([]);

  const onChange = (checkedValues) => { 
    setPermissionValue(checkedValues);
  };

  useEffect(() => {
    setFormData(prodd);
  }, [prodd]);

  async function fetchMyUsersData() {
    await dispatch(fetchMyUsers());
  }
 

  const onFinish = async (data) => {
    const res = await dispatch(
      save({
        url: `api/v2/users/${prodd?.usrId}/permissions`,
        permissionValue,
      })
    );
    // permission:permissionValue?.join(","),
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await fetchMyUsersData();
      await form.resetFields();
      await setIsModalOpen(false);
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };
 

  useEffect(() => {
    if (prodd?.permissions) {
      setPermissionValue(prodd.permissions);
    }
  }, [prodd]);
  return (
    <>
      <Modal
        className=""
        title={`Assign permissions to ${prodd?.firstname}`}
        open={isModalOpen}
        onOk={onFinish}
        onCancel={handleCancel}
        width={800}
      >
        <div className="lg:px-5 px-3">
          <div
            className={`permission_title rounded-[30px] px-[1.5rem] py-1
         ${
           prodd?.role === "USER"
             ? "bg-green !text-white"
             : prodd?.role === "MANAGER"
             ? "bg-[#FFFF00] !text-[#152238]"
             : prodd?.role === "ADMIN"
             ? "bg-[#152238] !text-white"
             : ""
         } 
           inline-flex items-center justify-center`}
          >
            {prodd?.role}
          </div>

          <div className="flex  items-center gap-x-5 label_2 mt-4">
            <div className="w-[50px] h-[50px] rounded-full flex items-center justify-center bg-lightBlue text-blue">
              {prodd?.usrLogo ? (
                <img src={prodd?.usrLogo} alt="logo" />
              ) : (
                getLetterWord(`${prodd?.firstname} ${prodd?.lastname}`)
              )}
            </div>
            {prodd?.firstname} {prodd?.lastname}
          </div>

          <div className="label_2 mt-5">Permissions</div>
          <Divider className="mt-3" />

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
            <div className="grid lg:grid-cols-2 grid-cols-1 lg:gap-x-10">
              <Form.Item
                rules={[
                  {
                    required: true,
                    message: "Please select a permission",
                  },
                ]}
              >
                <Checkbox.Group
                  className="flex flex-col gap-y-3 check-permission"
                  options={permissionData?.permission}
                  value={permissionValue}
                  onChange={onChange}
                />
              </Form.Item>
            </div>

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
                    {saving ? <Spin /> : "Assign Permission"}
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
export default UserPermissionModal;
