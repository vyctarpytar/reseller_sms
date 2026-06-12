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
import {  fetchInvoiceStatus } from "../../features/filter/filterSlice";
import { fetchSavedSms, save } from "../../features/save/saveSlice";
import moment from "moment";
import { fetchInvoices } from "../../features/invoice/invoiceSlice";

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

  const { invoiceStatusData } = useSelector((state) => state.filterSms); 
  const { loading } = useSelector((state) => state.inv);
 
  const items = [
    {
      label: "Date",
      key: "Date",
    },
    {
      label: "Status",
      key: "Status",
    },
    {
      label: "Mobile",
      key: "Mobile",
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
    if (itemKey === "Date") {
      const newFilter = {
        type: "Date",
        value: "Date",
      };
      setFilters([...filters, newFilter]);
    } else if (itemKey === "Mobile") {
      const newFilter = {
        type: "Mobile",
        value: "Mobile",
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
      if (filters[index].type === "Date") {
        delete updatedData.invoDate;
      } else if (filters[index].type === "Mobile") {
        delete updatedData.invoPayerMobileNumber;
      } else if (filters[index].type === "Status") {
        delete updatedData.invoStatus;
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

  function onMonthChange(value) {
    setFormData((prevData) => ({
      ...prevData,
      invoDate: value,
    }));
  }

  const onChange = async (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  function fetchDistinctInvoiceData() {
    dispatch(fetchInvoiceStatus());
  }
 
  const onFinish = async (data) => {
    const res = await dispatch(
      fetchInvoices({
        invoStatus: formData?.invoStatus,
        invoDate: formData?.invoDate,
        invoPayerMobileNumber: formData?.invoPayerMobileNumber,
        url: "api/v2/credit/invoice-list",
      })
    );
    if (res?.payload?.success) {
      toast.success(res.payload?.messages?.message);
      setIsModalOpen(false);
    } else {
      toast.error(res.payload?.messages?.message);
    }
  };
  async function fetchInvoiceData() {
    const res = await dispatch(
      fetchInvoices({
        url: "api/v2/credit/invoice-list",
      })
    );
  }

  const handleClear = async () => {
    await setFilters([]);
    await setFormData({});
    await fetchInvoiceData();
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
                {filter.type === "Date" && (
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
                        name="invoDate"
                        style={{
                          width: "100%",
                        }}
                        placeholder={
                          formData?.invoDate
                            ? moment(formData?.invoDate)?.format(
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
                {filter.type === "Mobile" && (
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
                        name="invoPayerMobileNumber"
                        className="input"
                        onChange={onChange}
                        value={formData?.invoPayerMobileNumber}
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
                        name="invoStatus"
                        value={formData?.invoStatus}
                        className=""
                        allowClear
                        placeholder="Select Status"
                        style={{
                          width: "100%",
                        }}
                        onChange={(value) => {
                          handleSelectChange(value, "invoStatus");
                        }}
                        options={
                          invoiceStatusData?.length > 0 &&
                          invoiceStatusData?.map((item) => ({
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
