import React, { useEffect, useRef, useState } from "react";
import { Button, Card, Form, Input, Modal, Spin, Select } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { formatImgPath } from "../../utils";
import { accessLevelData, statusData } from "../../data";
import { save } from "../../features/save/saveSlice";
import { fetchMyUsers, fetchRoles } from "../../features/auth/authSlice";
import PhoneInput from "react-phone-input-2";
import { fetchUsersStatus } from "../../features/filter/filterSlice";

const UsersAddModal = ({ isModalOpen, setIsModalOpen, prodd }) => {
  const handleOk = () => {
    setIsModalOpen(false);
  };
  const handleCancel = () => {
    setIsModalOpen(false);
  };
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const { user, usersLoading, rolesData } = useSelector((state) => state.auth);
  const { saving } = useSelector((state) => state.save);
  const { userStatus } = useSelector((state) => state.filterSms);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  function closeModal() {
    toast.dismiss();
  }

  const [formData, setFormData] = useState({ ...prodd });

  const [loadingAttach, setLoadingAttach] = useState(false);
  const [profile, setProfile] = useState(
    formData?.usrIcon ? formData?.usrIcon : ""
  );

  useEffect(() => {
    setProfile(formData?.usrIcon ? formData?.usrIcon : "");
  }, [formData?.usrIcon]);

  function handleSelectChange(value, formName) {
    setFormData((prevData) => ({
      ...prevData,
      [formName]: value,
    }));
  }

  const [data, setdata] = useState({});

  const handleNumberChange = (e) => {
    setFormData((prevData) => ({
      ...prevData,
      phoneNumber: String(e),
    }));
  };

  useEffect(() => {
    setFormData(prodd);
  }, [prodd]);

  const onChange = async (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  async function fetchMyUsersData() {
    await dispatch(
      fetchMyUsers({
        url: "api/v2/users/get",   
      })
    );
  }

  const onFinish = async (data) => {
    const res = await dispatch(
      save({
        url: "api/v2/users",
        usrId: prodd?.usrId ? prodd?.usrId : null,
        firstname: formData?.firstname,
        lastname: formData?.lastname,
        email: formData?.email,
        phoneNumber: formData?.phoneNumber,
        usrNationalId: formData?.usrNationalId,
        usrStatus: formData?.usrStatus,
        role: formData?.role,
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await fetchMyUsersData();
      await form.resetFields();
      await setIsModalOpen(false);
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  async function fetchRolesData() {
    dispatch(fetchRoles());
  }

  function fetchDistinctStatusData() {
    dispatch(fetchUsersStatus());
  }

  // useEffect(() => {
  //   fetchDistinctStatusData();
  // }, []);

  return (
    <>
      <Modal
        className=""
        title={`${prodd?.usrId ? `Update ${prodd?.firstname}` : `New User`}`}
        open={isModalOpen}
        onOk={onFinish}
        onCancel={handleCancel}
        width={800}
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
          <div className="grid lg:grid-cols-2 grid-cols-1 lg:gap-x-10">
            <Form.Item
              label={
                <span>
                  First Name<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add first name",
                },
              ]}
            >
              <Input
                required
                name="firstname"
                onChange={onChange}
                value={formData?.firstname}
                className="input"
              />
            </Form.Item>
            <Form.Item
              label={
                <span>
                  Last Name<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add last name",
                },
              ]}
            >
              <Input
                required
                name="lastname"
                onChange={onChange}
                value={formData?.lastname}
                className="input"
              />
            </Form.Item>

            <Form.Item
              label={
                <span>
                  Email<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add email address",
                },
                {
                  type: "email",
                  message: "Please enter a valid email address",
                },
              ]}
            >
              <Input
                required
                name="email"
                onChange={onChange}
                value={formData?.email}
                type="email"
                className="input"
              />
            </Form.Item>

            <Form.Item
              label={
                <span>
                  Phone No.<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add office phone no",
                },
              ]}
            >
              <PhoneInput
                required
                name="phoneNumber"
                onChange={handleNumberChange}
                value={formData?.phoneNumber}
                country="ke"
                onlyCountries={["ke"]}
                countryCodeEditable={false}
                className="input rounded-[6px] border !border-[#cacaca] !h-[42px]"
              />
            </Form.Item>

            <Form.Item
              label={
                <span>
                  National ID<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add national identification no",
                },
              ]}
            >
              <Input
                required
                name="usrNationalId"
                onChange={onChange}
                value={formData?.usrNationalId}
                type="number"
                maxLength={8}
                minLength={6}
                className="input"
              />
            </Form.Item>

            <Form.Item
              className=""
              label={
                <span>
                  Access level <span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please select access level",
                },
              ]}
            >
              <Select
                name="role"
                className=""
                allowClear
                style={{
                  width: "100%",
                }}
                onChange={(value) => {
                  handleSelectChange(value, "role");
                }}
                value={formData?.role}
                options={
                  rolesData?.length > 0 &&
                  rolesData?.map((item) => ({
                    value: item?.rlName,
                    label: item?.rlName,
                  }))
                }
                showSearch
                optionFilterProp="children"
                filterOption={(input, option) =>
                  (option?.label?.toLocaleLowerCase() ?? "").includes(
                    input?.toLocaleLowerCase()
                  )
                }
                onDropdownVisibleChange={() => {
                  fetchRolesData();
                }}
              />
            </Form.Item>

            {prodd?.usrId && (
              <Form.Item
                className=""
                label={
                  <span>
                    Status<span className="text-[#FF0000]">*</span>
                  </span>
                }
                rules={[
                  {
                    required: true,
                    message: "Please select status",
                  },
                ]}
              >
                <Select
                  name="usrStatus"
                  className=""
                  allowClear
                  style={{
                    width: "100%",
                  }}
                  onChange={(value) => {
                    handleSelectChange(value, "usrStatus");
                  }}
                  value={formData?.usrStatus}
                  options={
                    userStatus?.length > 0 &&
                    userStatus?.map((item) => ({
                      value: item?.usrStatus,
                      label: item?.usrStatus,
                    }))
                  }
                  showSearch
                  optionFilterProp="children"
                  filterOption={(input, option) =>
                    (option?.label?.toLocaleLowerCase() ?? "").includes(
                      input?.toLocaleLowerCase()
                    )
                  }
                  onDropdownVisibleChange={() => {
                    fetchDistinctStatusData();
                  }}
                />
              </Form.Item>
            )}
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

              <div className="w-[150px]">
                <button key="submit" type="submit" className="cstm-btn">
                  {saving ? <Spin /> : prodd?.usrId ? "Update" : "Create"}
                </button>
              </div>
            </div>
          </div>
        </Form>
      </Modal>
    </>
  );
};
export default UsersAddModal;
