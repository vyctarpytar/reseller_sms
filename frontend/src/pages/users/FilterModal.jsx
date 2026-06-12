import React, { useEffect, useRef, useState } from "react";
import {
  Button,
  DatePicker,
  Dropdown,
  Form,
  Input,
  Modal,
  Select,
  Spin,
} from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import remove from "../../assets/svg/delete.svg";
import svg37 from "../../assets/svg/svg37.svg";
import svg25 from "../../assets/svg/svg25.svg";
import {
  fetchDistinctSenderNames,
  fetchDistinctStatus,
  fetchUsersStatus,
} from "../../features/filter/filterSlice";
import { fetchSavedSms, save } from "../../features/save/saveSlice";
import moment from "moment";
import { fetchResellerAccounts } from "../../features/reseller-account/resellerAccountSlice";
import { fetchMyUsers, fetchRoles } from "../../features/auth/authSlice";

const { TextArea } = Input;
const FilterModal = ({
  isModalOpen,
  setIsModalOpen,
  prodd,
  formData,
  setFormData,
}) => {
  const handleOk = () => {
    setIsModalOpen(false);
  };
  const handleCancel = () => {
    setIsModalOpen(false);
  };
  const [form] = Form.useForm();
  const formRef = useRef(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const { userStatus, senderNamesData } = useSelector(
    (state) => state.filterSms
  );
  const { loadingSms } = useSelector((state) => state.save);
  const { rolesData } = useSelector((state) => state.auth);

  const items = [
    {
      label: "First Name",
      key: "First Name",
    },
    {
      label: "Last Name",
      key: "Last Name",
    },
    {
      label: "Email",
      key: "Email",
    },
    {
      label: "Phone Number",
      key: "Phone Number",
    },
    {
      label: "National ID",
      key: "National ID",
    },
    {
      label: "Status",
      key: "Status",
    },

    {
      label: "Role",
      key: "Role",
    },
  ];

  const [filters, setFilters] = useState([]);

  const filteredItems = items.filter(
    (item) => !filters?.some((filter) => filter?.value === item?.key)
  );

  const settingItems = filteredItems?.map((item) => ({
    key: item?.key,
    label: (
      <div
        className="font-dmSans text-black font-[400] text-[19px] mt-[5%]"
        onClick={() => handleMenuItemClick(item?.key)}
      >
        {item?.label}
      </div>
    ),
  }));

  const handleMenuItemClick = (itemKey) => {
    if (itemKey === "First Name") {
      const newFilter = {
        type: "First Name",
        value: "First Name",
      };
      setFilters([...filters, newFilter]);
    } else if (itemKey === "last Name") {
      const newFilter = {
        type: "last Name",
        value: "last Name",
      };
      setFilters([...filters, newFilter]);
    } else if (itemKey === "Email") {
      const newFilter = {
        type: "Email",
        value: "Email",
      };
      setFilters([...filters, newFilter]);
    } else if (itemKey === "Phone Number") {
      const newFilter = {
        type: "Phone Number",
        value: "Phone Number",
      };
      setFilters([...filters, newFilter]);
    } else if (itemKey === "National ID") {
      const newFilter = {
        type: "National ID",
        value: "National ID",
      };
      setFilters([...filters, newFilter]);
    } else if (itemKey === "Status") {
      const newFilter = {
        type: "Status",
        value: "Status",
      };
      setFilters([...filters, newFilter]);
    } else if (itemKey === "Role") {
      const newFilter = {
        type: "Role",
        value: "Role",
      };
      setFilters([...filters, newFilter]);
    }
  };
  const removeFilter = (index) => {
    const updatedFilters = filters?.filter((_, idx) => idx !== index);
    setFilters(updatedFilters);
    setFormData((prevData) => {
      const updatedData = { ...prevData };
      if (filters[index].type === "First Name") {
        delete updatedData.firstname;
      } else if (filters[index].type === "Last Name") {
        delete updatedData.lastname;
      } else if (filters[index].type === "Email") {
        delete updatedData.email;
      } else if (filters[index].type === "Phone Number") {
        delete updatedData.phoneNumber;
      } else if (filters[index].type === "National ID") {
        delete updatedData.usrNationalId;
      } else if (filters[index].type === "Status") {
        delete updatedData.usrStatus;
      } else if (filters[index].type === "Role") {
        delete updatedData.role;
      }
      return updatedData;
    });
  };

  // const [formData, setFormData] = useState();

  function handleSelectChange(value, formName) {
    setFormData((prevData) => ({
      ...prevData,
      [formName]: value,
    }));
  }

  const onChange = async (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  function fetchDistinctStatusData() {
    dispatch(fetchUsersStatus());
  }

  const onFinish = async (data) => {
    const res = await dispatch(
      fetchMyUsers({
        url: "api/v2/users/get", 
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
      toast.success(res.payload?.messages?.message);
      setIsModalOpen(false);
    } else {
      toast.error(res.payload?.messages?.message);
    }
  };
  async function fetchUsersData() {
    const res = await dispatch(
      fetchMyUsers({
        url: "api/v2/users/get", 
      })
    );
  }

  const handleClear = async () => {
    await setFilters([]);
    await setFormData({});
    await fetchUsersData();
    await handleCancel();
  };

  async function fetchRolesData() {
    dispatch(fetchRoles());
  }

  return (
    <>
      <Modal
        className="!mt-[13%] filter-modal "
        title="New Account "
        open={isModalOpen}
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
          <div className="flex justify-between text-black21 text-[18px] font-normal leading-[24px] font-dmSans mb-5">
            <div className="">Filters</div>
            <div className="cursor-pointer" onClick={handleClear}>
              Clear
            </div>
          </div>
          <div className="grid grid-cols-1">
            <span className="text-black21 text-[18px] font-normal leading-[24px] font-dmSans mt-5">
              All filters
            </span>
            {filters.map((filter, index) => (
              <div
                key={index}
                className="flex flex-col items-start mt-4 w-full"
              >
                {filter.type === "First Name" && (
                  <div className="flex items-center w-full gap-x-2">
                    <Form.Item className="w-[100%]">
                      <Input
                        className="input"
                        name={`date-${index}`}
                        value={filter.value}
                        readOnly={true}
                      />
                    </Form.Item>

                    <Form.Item className="w-[100%]">
                      <Input
                        name="firstname"
                        className="input"
                        onChange={onChange}
                        value={formData?.firstname}
                        placeholder="Enter First Name."
                      />
                    </Form.Item>
                    <Form.Item className="w-[20%]">
                      <button type="button" onClick={() => removeFilter(index)}>
                        <img src={remove} alt="remove-icon" />
                      </button>
                    </Form.Item>
                  </div>
                )}
                {filter.type === "Last Name" && (
                  <div className="flex items-center w-full gap-x-2">
                    <Form.Item className="w-[100%]">
                      <Input
                        className="input"
                        name={`input-${index}`}
                        value={filter.value}
                        readOnly={true}
                      />
                    </Form.Item>
                    <Form.Item className="w-[100%]">
                      <Input
                        name="lastname"
                        className="input"
                        onChange={onChange}
                        value={formData?.lastname}
                        placeholder="Enter Last Name."
                      />
                    </Form.Item>
                    <Form.Item className="w-[20%]">
                      <button onClick={() => removeFilter(index)}>
                        <img src={remove} alt="remove-icon" />
                      </button>
                    </Form.Item>
                  </div>
                )}

                {filter.type === "Email" && (
                  <div className="flex items-center w-full gap-x-2">
                    <Form.Item className="w-[100%]">
                      <Input
                        className="input"
                        name={`input-${index}`}
                        value={filter.value}
                        readOnly={true}
                      />
                    </Form.Item>
                    <Form.Item className="w-[100%]">
                      <Input
                        name="email"
                        className="input"
                        onChange={onChange}
                        value={formData?.email}
                        placeholder="Enter email."
                      />
                    </Form.Item>
                    <Form.Item className="w-[20%]">
                      <button onClick={() => removeFilter(index)}>
                        <img src={remove} alt="remove-icon" />
                      </button>
                    </Form.Item>
                  </div>
                )}

                {filter.type === "Phone Number" && (
                  <div className="flex items-center w-full gap-x-2">
                    <Form.Item className="w-[100%]">
                      <Input
                        className="input"
                        name={`input-${index}`}
                        value={filter.value}
                        readOnly={true}
                      />
                    </Form.Item>
                    <Form.Item className="w-[100%]">
                      <Input
                        name="phoneNumber"
                        className="input"
                        onChange={onChange}
                        value={formData?.phoneNumber}
                        placeholder="Enter phone."
                      />
                    </Form.Item>
                    <Form.Item className="w-[20%]">
                      <button onClick={() => removeFilter(index)}>
                        <img src={remove} alt="remove-icon" />
                      </button>
                    </Form.Item>
                  </div>
                )}
                {filter.type === "National ID" && (
                  <div className="flex items-center w-full gap-x-2">
                    <Form.Item className="w-[100%]">
                      <Input
                        className="input"
                        name={`input-${index}`}
                        value={filter.value}
                        readOnly={true}
                      />
                    </Form.Item>
                    <Form.Item className="w-[100%]">
                      <Input
                        name="usrNationalId"
                        className="input"
                        onChange={onChange}
                        value={formData?.usrNationalId}
                        placeholder="Enter National ID."
                      />
                    </Form.Item>
                    <Form.Item className="w-[20%]">
                      <button onClick={() => removeFilter(index)}>
                        <img src={remove} alt="remove-icon" />
                      </button>
                    </Form.Item>
                  </div>
                )}

                {filter.type === "Status" && (
                  <div className="flex items-center w-full gap-x-2">
                    <Form.Item className="w-[100%]">
                      <Input
                        className="input"
                        name={`input-${index}`}
                        value={filter.value}
                        readOnly={true}
                      />
                    </Form.Item>
                    <Form.Item className="w-[100%]">
                      <Select
                        name="usrStatus"
                        value={formData?.usrStatus}
                        className=""
                        allowClear
                        placeholder="Select Status"
                        style={{
                          width: "100%",
                        }}
                        onChange={(value) => {
                          handleSelectChange(value, "usrStatus");
                        }}
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
                    <Form.Item className="w-[20%]">
                      <button onClick={() => removeFilter(index)}>
                        <img src={remove} alt="remove-icon" />
                      </button>
                    </Form.Item>
                  </div>
                )}
                {filter.type === "Role" && (
                  <div className="flex items-center w-full gap-x-2">
                    <Form.Item className="w-[100%]">
                      <Input
                        className="input"
                        name={`input-${index}`}
                        value={filter.value}
                        readOnly={true}
                      />
                    </Form.Item>
                    <Form.Item className="w-[100%]">
                      <Select
                        name="role"
                        value={formData?.role}
                        className=""
                        allowClear
                        placeholder="Select Role"
                        style={{
                          width: "100%",
                        }}
                        onChange={(value) => {
                          handleSelectChange(value, "role");
                        }}
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
                    <Form.Item className="w-[20%]">
                      <button onClick={() => removeFilter(index)}>
                        <img src={remove} alt="remove-icon" />
                      </button>
                    </Form.Item>
                  </div>
                )}
              </div>
            ))}

            <div className="flex justify-between items-center">
              <Dropdown
                overlayStyle={{
                  width: "200px",
                }}
                trigger={"click"}
                menu={{ items: settingItems }}
                placement="bottom"
              >
                <Button
                  type="btn"
                  className="flex items-center w-[150px] bg-[#A3A2A7] text-white h-[45px] py-3 px-2
                 rounded-[10px] gap-x-1 mb-10 mt-5 !text-lexendS text-[16px] font-[500]"
                >
                  <img src={svg25} alt="add-icon" />
                  Add Filter
                  <img src={svg37} alt="svg37" />
                </Button>
              </Dropdown>

              {filters && filters?.length > 0 && (
                <div className="w-[150px]">
                  <button type="submit" className="cstm-btn">
                    {loadingSms ? <Spin /> : "Submit"}
                  </button>
                </div>
              )}
            </div>
          </div>
        </Form>
      </Modal>
    </>
  );
};
export default FilterModal;
