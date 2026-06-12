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
import remove from "../../../assets/svg/delete.svg";
import svg37 from "../../../assets/svg/svg37.svg";
import svg25 from "../../../assets/svg/svg25.svg";
import moment from "moment";
import { fetchResellerAccounts } from "../../../features/reseller-account/resellerAccountSlice";
import { fetchDailySmsReport } from "../../../features/dashboard/dashboardSlice";
import { disabledDate, formatDate, getDate7DaysAgo, normalizeDateToLocalYear } from "../../../utils";

const { TextArea } = Input;
const FilterDailySmsModal = ({
  isModalOpen,
  setIsModalOpen,
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

  const { loading } = useSelector((state) => state.dash);
  const { resellerAccountData } = useSelector((state) => state.resellerAccount);
  const { user } = useSelector((state) => state.auth);

  const items = [
    {
      label: "Date From ",
      key: "Date From",
    },
    {
      label: "Date To ",
      key: "Date To",
    },
    {
      label: "Account",
      key: "Account",
    },
  ];

  const [filters, setFilters] = useState([]);

  // const filteredItems = items.filter(
  //   (item) => !filters?.some((filter) => filter?.value === item?.key)
  // );

  const filteredItems = items.filter((item) => {
    const showAccount = user?.layer != "ACCOUNT" || item?.key !== "Account";
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
    console.log("itemKey", itemKey);
    if (itemKey === "Date From") {
      const newFilter = {
        type: "Date From",
        value: "Date From",
      };
      setFilters([...filters, newFilter]);
    } else if (itemKey === "Date To") {
      const newFilter = {
        type: "Date To",
        value: "Date To",
      };
      setFilters([...filters, newFilter]);
    } else if (itemKey === "Account") {
      const newFilter = {
        type: "Account",
        value: "Account",
      };
      setFilters([...filters, newFilter]);
    }
  };
  const removeFilter = (index) => {
    const updatedFilters = filters?.filter((_, idx) => idx !== index);
    setFilters(updatedFilters);
    setFormData((prevData) => {
      const updatedData = { ...prevData };
      if (filters[index].type === "Date From") {
        delete updatedData.msgDateFrom;
      } else if (filters[index].type === "Date To") {
        delete updatedData.msgDateTo;
      } else if (filters[index].type === "Account") {
        delete updatedData.msgAccId;
      }
      return updatedData;
    });
  };

  function onMonthChangeFrom(value) {
    setFormData((prevData) => ({
      ...prevData,
      msgDateFrom: value,
    }));
  }
  function onMonthChangeTo(value) {
    setFormData((prevData) => ({
      ...prevData,
      msgDateTo: value,
    }));
  }

  const onChange = async (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };
  function handleSelectChange(value, formName) {
    setFormData((prevData) => ({
      ...prevData,
      [formName]: value,
    }));
  }
  function fetchResellerAccountData() {
    dispatch(fetchResellerAccounts());
  }

  const onFinish = async (data) => {
    if (!formData?.msgDateFrom) {
      toast.error("Select date from");
      return;
    }
    if (!formData?.msgDateTo) {
      toast.error("Select date to");
      return;
    }
    const res = await dispatch(
      fetchDailySmsReport({
        msgDateFrom: normalizeDateToLocalYear(formData?.msgDateFrom),
        msgDateTo: normalizeDateToLocalYear(formData?.msgDateTo),
        msgAccId: user?.layer != "ACCOUNT" ? formData?.msgAccId : null,
        url: "api/v2/rpt/daily-sms-usage",
      })
    );
    if (res?.payload?.success) {
      // toast.success(res.payload?.messages?.message);
      setIsModalOpen(false);
    } else {
      toast.error(res.payload?.messages?.message);
    }
  };
  const today = new Date();
  async function fetchDailySmsData() {
    const res = await dispatch(
      fetchDailySmsReport({
        url: "api/v2/rpt/daily-sms-usage",
        msgDateFrom: formData?.msgDateFrom ?? getDate7DaysAgo(),
        msgDateTo: formData?.msgDateTo ?? formatDate(today),
        msgAccId: user?.layer != "ACCOUNT" ? formData?.msgAccId : null,
      })
    );
  }

  const handleClear = async () => {
    await setFilters([]);
    await setFormData({});
    await form.resetFields()
    await fetchDailySmsData();
    await handleCancel();
  };

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
                {filter.type === "Date From" && (
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
                        name="msgDateFrom"
                        style={{
                          width: "100%",
                        }}
                        placeholder={
                          formData?.msgDateFrom
                            ? moment(formData?.msgDateFrom)?.format(
                                "DD-MM-YYYY"
                              )
                            : "Select Date"
                        }
                        className="input"
                        format={"YYYY-MM-DD"}
                        onChange={onMonthChangeFrom}
                        disabledDate={disabledDate}
                      />
                    </Form.Item>
                    <Form.Item className="w-[20%]">
                      <button type="button" onClick={() => removeFilter(index)}>
                        <img src={remove} alt="remove-icon" />
                      </button>
                    </Form.Item>
                  </div>
                )}
                {filter.type === "Date To" && (
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
                        name="msgDateTo"
                        style={{
                          width: "100%",
                        }}
                        placeholder={
                          formData?.msgDateTo
                            ? moment(formData?.msgDateTo)?.format("DD-MM-YYYY")
                            : "Select Date"
                        }
                        className="input"
                        format={"YYYY-MM-DD"}
                        onChange={onMonthChangeTo}
                        disabledDate={disabledDate}
                      />
                    </Form.Item>
                    <Form.Item className="w-[20%]">
                      <button type="button" onClick={() => removeFilter(index)}>
                        <img src={remove} alt="remove-icon" />
                      </button>
                    </Form.Item>
                  </div>
                )}
                {filter.type === "Account" && (
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
                        name="msgAccId"
                        value={formData?.msgAccId}
                        className=""
                        allowClear
                        placeholder="Select Account"
                        style={{
                          width: "100%",
                        }}
                        onChange={(value) => {
                          handleSelectChange(value, "msgAccId");
                        }}
                        options={
                          resellerAccountData?.length > 0 &&
                          resellerAccountData?.map((item) => ({
                            value: item?.accId,
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
                  <button disabled={loading} type="submit" className="cstm-btn">
                    {loading ? <Spin /> : "Submit"}
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
export default FilterDailySmsModal;
