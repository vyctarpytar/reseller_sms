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
  fetchCreditStatus,
  fetchUsersStatus,
} from "../../features/filter/filterSlice";
import { fetchMyUsers, fetchRoles } from "../../features/auth/authSlice";
import moment from "moment";
import { fetchCreditReseller } from "../../features/credit/creditSlice";
import { fetchResellerAccounts } from "../../features/reseller-account/resellerAccountSlice";

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

  const { userStatus, creditStatus } = useSelector(
    (state) => state.filterSms
  );
  const { loadingSms } = useSelector((state) => state.save);
  const { user } = useSelector((state) => state.auth);
  const { resellerAccountData } = useSelector((state) => state.resellerAccount);

  function onMonthChange(value) {
    setFormData((prevData) => ({
      ...prevData,
      smsCreatedDate: value,
    }));
  }

  const items = [
    {
      label: "Created Date",
      key: "Created Date",
    },
    // {
    //   label: "Reseller Name",
    //   key: "Reseller Name",
    // },
    {
      label: "Account Name",
      key: "Account Name",
    }, 
    {
      label: "Status",
      key: "Status",
    }, 
  ];

  const [filters, setFilters] = useState([]);

  const filteredItems = items.filter((item) => {
    const showAccount = user?.layer != "ACCOUNT" || item?.key !== "Account Name";
    return (
      showAccount && !filters?.some((filter) => filter?.value === item?.key)
    );
  });

  

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
    if (itemKey === "Created Date") {
      const newFilter = {
        type: "Created Date",
        value: "Created Date",
      };
      setFilters([...filters, newFilter]);
    }
    // else if (itemKey === "Reseller Name") {
    //   const newFilter = {
    //     type: "Reseller Name",
    //     value: "Reseller Name",
    //   };
    //   setFilters([...filters, newFilter]);
    // } 
    else if (itemKey === "Account Name") {
      const newFilter = {
        type: "Account Name",
        value: "Account Name",
      };
      setFilters([...filters, newFilter]);
    } else if (itemKey === "Status") {
      const newFilter = {
        type: "Status",
        value: "Status",
      };
      setFilters([...filters, newFilter]);
    }  
  };
  const removeFilter = (index) => {
    const updatedFilters = filters?.filter((_, idx) => idx !== index);
    setFilters(updatedFilters);
    setFormData((prevData) => {
      const updatedData = { ...prevData };
      if (filters[index].type === "Created Date") {
        delete updatedData.smsCreatedDate;
      } 
      // else if (filters[index].type === "Reseller Name") {
      //   delete updatedData.smsResellerName;
      // } 
      else if (filters[index].type === "Account Name") {
        delete updatedData.smsAccountName;
      } else if (filters[index].type === "Status") {
        delete updatedData.usrStatus;
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
      fetchCreditReseller({
     url:user?.layer === "TOP" ? "api/v2/credit/reseller" : user?.layer === "RESELLER" ? "api/v2/credit/reseller-account": user?.layer === "ACCOUNT"? "api/v2/credit/account" : null,
        // smsResellerName: formData?.smsResellerName,
        smsAccountName: user?.layer != "ACCOUNT" ? formData?.smsAccountName : null,
        smsCreatedByName: formData?.smsCreatedByName,
        smsCreatedDate: formData?.smsCreatedDate,
        smsApprovedByName: formData?.smsApprovedByName,
        smsApprovedDate: formData?.smsApprovedDate,
        crStatus: formData?.crStatus,
      })
    ); 
    if (res?.payload?.success) {
      // toast.success(res.payload?.messages?.message);
      setIsModalOpen(false);
    } else {
      // toast.error(res.payload?.messages?.message);
    }
  };
  async function fetchCreditData() {
    const res = await dispatch(
      fetchCreditReseller({
     url:user?.layer === "TOP" ? "api/v2/credit/reseller" : user?.layer === "RESELLER" ? "api/v2/credit/reseller-account": user?.layer === "ACCOUNT"? "api/v2/credit/account" : null, 
      })
    );
  }

  const handleClear = async () => {
    await setFilters([]);
    await setFormData({});
    await fetchCreditData();
    await handleCancel();
  };

  async function fetchCreditStatusData() {
    dispatch(fetchCreditStatus());
  }
  function fetchResellerAccountData() {
    dispatch(fetchResellerAccounts());
  }

  useEffect(()=>{
    fetchCreditStatusData()
  },[])

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
                  {filter.type === "Created Date" && (
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
                      <DatePicker
                        name="smsCreatedDate"
                        style={{
                          width: "100%",
                        }}
                        placeholder={
                          formData?.smsCreatedDate
                            ? moment(formData?.smsCreatedDate)?.format(
                                "DD-MM-YYYY"
                              )
                            : "Select Date"
                        }
                        className="input"
                        format={"YYYY-MM-DD"}
                        onChange={onMonthChange}
                      />
                    </Form.Item>
                    <Form.Item className="w-[20%]">
                      <button type="button" onClick={() => removeFilter(index)}>
                        <img src={remove} alt="remove-icon" />
                      </button>
                    </Form.Item>
                  </div>
                )}
                {/* {filter.type === "Reseller Name" && (
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
                        name="smsResellerName"
                        className="input"
                        onChange={onChange}
                        value={formData?.smsResellerName}
                        placeholder="Enter Reseller Name."
                      />
                    </Form.Item>
                    <Form.Item className="w-[20%]">
                      <button type="button" onClick={() => removeFilter(index)}>
                        <img src={remove} alt="remove-icon" />
                      </button>
                    </Form.Item>
                  </div>
                )} */}
                {filter.type === "Account Name" && (
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
                        name="smsAccountName"
                        value={formData?.smsAccountName}
                        className=""
                        allowClear
                        placeholder="Select Account"
                        style={{
                          width: "100%",
                        }}
                        onChange={(value) => {
                          handleSelectChange(value, "smsAccountName");
                        }}
                        options={
                          resellerAccountData?.length > 0 &&
                          resellerAccountData?.map((item) => ({
                            value: item?.accName,
                            label: item?.accName,
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
                          fetchResellerAccountData();
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
                        name="crStatus"
                        value={formData?.crStatus}
                        className=""
                        allowClear
                        placeholder="Select Status"
                        style={{
                          width: "100%",
                        }}
                        onChange={(value) => {
                          handleSelectChange(value, "crStatus");
                        }}
                        options={
                          creditStatus?.length > 0 &&
                          creditStatus?.map((item) => ({
                            value: item,
                            label: item,
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
                          // fetchCreditStatusData();
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
