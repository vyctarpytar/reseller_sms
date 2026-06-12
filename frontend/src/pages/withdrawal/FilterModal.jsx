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
import { recurringBillsData } from "../../data";
import add from "../../assets/svg/add-dark.svg";
import remove from "../../assets/svg/delete.svg";
import svg37 from "../../assets/svg/svg37.svg";
import svg25 from "../../assets/svg/svg25.svg"; 
import moment from "moment"; 
import { fetchPayoutDistinctStatus, fetchPayoutHistory } from "../../features/billing/billingSlice";

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

  const { payoutDistinctStatus } = useSelector((state) => state.billing); 
  const { loading } = useSelector((state) => state.inv);
 
  const items = [
    {
      label: "Created Date",
      key: "Created Date",
    },
    // {
    //   label: "Date To",
    //   key: "Date To",
    // },
    {
      label: "Status",
      key: "Status",
    },
    {
      label: "Payout Mobile",
      key: "Payout Mobile",
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
    if (itemKey === "Created Date") {
      const newFilter = {
        type: "Created Date",
        value: "Created Date",
      };
      setFilters([...filters, newFilter]);
    }
    // else if (itemKey === "Date To") {
    //   const newFilter = {
    //     type: "Date To",
    //     value: "Date To",
    //   };
    //   setFilters([...filters, newFilter]);
    // } 
    else if (itemKey === "Payout Mobile") {
      const newFilter = {
        type: "Payout Mobile",
        value: "Payout Mobile",
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
        delete updatedData.withDrawCreatedDate;
      } 
      // else if (filters[index].type === "Date To") {
      //   delete updatedData.withDrawDateTo;
      // } 
      else if (filters[index].type === "Payout Mobile") {
        delete updatedData.withDrawSubmobileNo;
      } else if (filters[index].type === "Status") {
        delete updatedData.withDrawStatus;
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

  function onMonthChangeFrom(value) {
    setFormData((prevData) => ({
      ...prevData,
      withDrawCreatedDate: value,
    }));
  }

  function onMonthChangeTo(value) {
    setFormData((prevData) => ({
      ...prevData,
      withDrawDateTo: value,
    }));
  }

  const onChange = async (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  function fetchDistinctInvoiceData() {
    dispatch(fetchPayoutDistinctStatus());
  }
 
  const onFinish = async (data) => {
    const res = await dispatch(
      fetchPayoutHistory({
        withDrawStatus: formData?.withDrawStatus,
        withDrawCreatedDate: formData?.withDrawCreatedDate,
        // withDrawDateTo: formData?.withDrawDateTo,
        withDrawSubmobileNo: formData?.withDrawSubmobileNo,
        url: "api/v2/wallet/withdrawals",
      })
    );
    if (res?.payload?.success) {
      toast.success(res.payload?.messages?.message);
      setIsModalOpen(false);
    } else {
      toast.error(res.payload?.messages?.message);
    }
  };
  async function fetchPayoutHistoryData() {
    const res = await dispatch(
      fetchPayoutHistory({
        url: "api/v2/wallet/withdrawals",
      })
    );
  }

  const handleClear = async () => {
    await setFilters([]);
    await setFormData({});
    await fetchPayoutHistoryData();
    await handleCancel()
  };

  useEffect(() => {
    fetchDistinctInvoiceData();
  }, []);

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
                        name="withDrawCreatedDate"
                        style={{
                          width: "100%",
                        }}
                        placeholder={
                          formData?.withDrawCreatedDate
                            ? moment(formData?.withDrawCreatedDate)?.format(
                                "DD-MM-YYYY"
                              )
                            : "Select Date"
                        }
                        className="input"
                        format={"YYYY-MM-DD"}
                        onChange={onMonthChangeFrom}
                      />
                    </Form.Item>
                    <Form.Item className="w-[20%]">
                      <button type="button" onClick={() => removeFilter(index)}>
                        <img src={remove} alt="remove-icon" />
                      </button>
                    </Form.Item>
                  </div>
                )}
                  {/* {filter.type === "Date To" && (
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
                        name="withDrawDateTo"
                        style={{
                          width: "100%",
                        }}
                        placeholder={
                          formData?.withDrawDateTo
                            ? moment(formData?.withDrawDateTo)?.format(
                                "DD-MM-YYYY"
                              )
                            : "Select Date"
                        }
                        className="input"
                        format={"YYYY-MM-DD"}
                        onChange={onMonthChangeTo}
                      />
                    </Form.Item>
                    <Form.Item className="w-[20%]">
                      <button type="button" onClick={() => removeFilter(index)}>
                        <img src={remove} alt="remove-icon" />
                      </button>
                    </Form.Item>
                  </div>
                )} */}
                {filter.type === "Payout Mobile" && (
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
                        name="withDrawSubmobileNo"
                        className="input"
                        onChange={onChange}
                        value={formData?.withDrawSubmobileNo}
                        placeholder="Enter Mobile No."
                        type="number"
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
                        name="withDrawStatus"
                        value={formData?.withDrawStatus}
                        className=""
                        allowClear
                        placeholder="Select Status"
                        style={{
                          width: "100%",
                        }}
                        onChange={(value) => {
                          handleSelectChange(value, "withDrawStatus");
                        }}
                        options={
                          payoutDistinctStatus?.length > 0 &&
                          payoutDistinctStatus?.map((item) => ({
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
                        onDropdownVisibleChange={() => {}}
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
                  className="flex items-center w-[150px] bg-[#69472E] text-white h-[45px] py-3 px-2
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
export default FilterModal;
