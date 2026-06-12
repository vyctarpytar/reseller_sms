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
import {
  fetchDistinctSenderNames,
  fetchDistinctStatus,
} from "../../features/filter/filterSlice";
import { fetchSavedSms, save } from "../../features/save/saveSlice";
import moment from "moment";
import { fetchResellerAccounts } from "../../features/reseller-account/resellerAccountSlice";
import { normalizeDateToLocalYearWTime } from "../../utils";

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

  const { statusData, senderNamesData } = useSelector(
    (state) => state.filterSms
  );
  const { loadingSms } = useSelector((state) => state.save);
  const { resellerAccountData } = useSelector((state) => state.resellerAccount);

  const items = [
    {
      label: "From",
      key: "From",
    },
    {
      label: "To",
      key: "To",
    },
    {
      label: "Status",
      key: "Status",
    },
    {
      label: "Mobile",
      key: "Mobile",
    },
    {
      label: "Message",
      key: "Message",
    },
    {
      label: "Account",
      key: "Account",
    },
    {
      label: "Sender",
      key: "Sender",
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
 
    if (itemKey === "From") {
      const newFilter = {
        type: "From",
        value: "From",
      };
      setFilters([...filters, newFilter]);
    }  else if (itemKey === "To") {
      const newFilter = {
        type: "To",
        value: "To",
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
    } else if (itemKey === "Message") {
      const newFilter = {
        type: "Message",
        value: "Message",
      };
      setFilters([...filters, newFilter]);
    } else if (itemKey === "Account") {
      const newFilter = {
        type: "Account",
        value: "Account",
      };
      setFilters([...filters, newFilter]);
    } else if (itemKey === "Sender") {
      const newFilter = {
        type: "Sender",
        value: "Sender",
      };
      setFilters([...filters, newFilter]);
    }
  };
  const removeFilter = (index) => {
    const updatedFilters = filters?.filter((_, idx) => idx !== index);
    setFilters(updatedFilters);
    setFormData((prevData) => {
      const updatedData = { ...prevData };
      if (filters[index].type === "From") {
        delete updatedData.msgCreatedFrom;
      }
      if (filters[index].type === "To") {
        delete updatedData.msgCreatedTo;
      }
      else if (filters[index].type === "Mobile") {
        delete updatedData.msgSubmobileNo;
      } else if (filters[index].type === "Status") {
        delete updatedData.msgStatus;
      } else if (filters[index].type === "Message") {
        delete updatedData.msgMessage;
      } else if (filters[index].type === "Account") {
        delete updatedData.msgAccId;
      } else if (filters[index].type === "Sender") {
        delete updatedData.msgSenderId;
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
      msgCreatedFrom: value,
    }));
  }
  function onMonthChangeTo(value) {
    setFormData((prevData) => ({
      ...prevData,
      msgCreatedTo: value,
    }));
  }
  const onChange = async (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  function fetchDistinctStatusData() {
    dispatch(fetchDistinctStatus());
  }

  function fetchResellerAccountData() {
    dispatch(fetchResellerAccounts());
  }

  function fetchDistinctSenderNamesData() {
    dispatch(fetchDistinctSenderNames());
  }

  const onFinish = async (data) => {
    const res = await dispatch(
      fetchSavedSms({
        msgStatus: formData?.msgStatus,
        msgCreatedFrom:normalizeDateToLocalYearWTime(formData?.msgCreatedFrom),
        msgCreatedTo:normalizeDateToLocalYearWTime(formData?.msgCreatedTo),
        msgSubmobileNo: formData?.msgSubmobileNo,
        msgMessage: formData?.msgMessage,
        msgAccId: formData?.msgAccId,
        msgSenderId: formData?.msgSenderId,
        url: "api/v2/sms",
      })
    );
    if (res?.payload?.success) {
      toast.success(res.payload?.messages?.message);
      setIsModalOpen(false);
    } else {
      toast.error(res.payload?.messages?.message);
    }
  };
  async function fetchSentSmsData() {
    const res = await dispatch(
      fetchSavedSms({
        url: "api/v2/sms",
      })
    );
  }

  const handleClear = async () => {
    await setFilters([]);
    await setFormData({});
    await fetchSentSmsData();
    await handleCancel();
  };

  useEffect(() => {
    fetchDistinctStatusData();
    fetchResellerAccountData();
    fetchDistinctSenderNamesData();
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
                {filter.type === "From" && (
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
                        name="msgCreatedFrom"
                        style={{
                          width: "100%",
                        }}
                        placeholder={
                          formData?.msgCreatedFrom
                            ? moment(formData?.msgCreatedFrom)?.format(
                                "DD-MM-YYYY"
                              )
                            : "Select From Date"
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
                 {filter.type === "To" && (
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
                        name="msgCreatedTo"
                        style={{
                          width: "100%",
                        }}
                        placeholder={
                          formData?.msgCreatedTo
                            ? moment(formData?.msgCreatedTo)?.format(
                                "DD-MM-YYYY"
                              )
                            : "Select To Date"
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
                        name="msgSubmobileNo"
                        className="input"
                        onChange={onChange}
                        value={formData?.msgSubmobileNo}
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
                        name="msgStatus"
                        value={formData?.msgStatus}
                        className=""
                        allowClear
                        placeholder="Select Status"
                        style={{
                          width: "100%",
                        }}
                        onChange={(value) => {
                          handleSelectChange(value, "msgStatus");
                        }}
                        options={
                          statusData?.length > 0 &&
                          statusData?.map((item) => ({
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

                {filter.type === "Message" && (
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
                        name="msgMessage"
                        className="input"
                        onChange={onChange}
                        value={formData?.msgMessage}
                        placeholder="Enter message."
                      />
                    </Form.Item>
                    <Form.Item className="w-[20%]">
                      <button onClick={() => removeFilter(index)}>
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

                {filter.type === "Sender" && (
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
                        name="msgSenderId"
                        value={formData?.msgSenderId}
                        className=""
                        allowClear
                        placeholder="Select Sender"
                        style={{
                          width: "100%",
                        }}
                        onChange={(value) => {
                          handleSelectChange(value, "msgSenderId");
                        }}
                        options={
                          senderNamesData?.length > 0 &&
                          senderNamesData?.map((item) => ({
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
                          fetchDistinctSenderNamesData()
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
