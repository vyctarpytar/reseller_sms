import React, { useEffect, useRef, useState } from "react";
import { DatePicker, Form, Input, Modal, Select, Spin } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { formatDate, removeCommas } from "../../utils";
import { save } from "../../features/save/saveSlice";
import { fetchSingleAccount } from "../../features/reseller-account/resellerAccountSlice";
import numeral from "numeral";
import { fetchinvoDistinctStatus, fetchInvoices } from "../../features/invoice/invoiceSlice";

const { TextArea } = Input;
const AccpetanceModal = ({ isModalOpen, setIsModalOpen, prodd, title,formData }) => {
  const handleOk = () => {
    setIsModalOpen(false);
  };
  const handleCancel = () => {
    setIsModalOpen(false);
  };
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const { user } = useSelector((state) => state.auth);
  const { saving } = useSelector((state) => state.save);
  const { invoDistinctStatus } = useSelector((state) => state.inv);

  const linkRef = useRef(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [data, setdata] = useState({});

  async function handleSelectChange(value, formName) {
    await setdata((prevData) => ({
      ...prevData,
      [formName]: value,
    }));
  }

  const [formData1,setFormData1] = useState()
  const onChange = (e) => {
    let { name, value } = e.target;
    const cleanedValue = value.replace(/[^0-9.]/g, "");
    const numericValue = Number(cleanedValue).toString();
    const formattedValue = numeral(numericValue).format("0,0");

    setFormData1((prev) => ({
      ...prev,
      [name]: formattedValue,
    }));
  };

  async function fetchInvoiceData() {
    const res = await dispatch(
      fetchInvoices({
        url: "api/v2/credit/invoice-list",  
      })
    );
  }

  async function fetchInvoiceDistinctData() {
    const res = await dispatch(fetchinvoDistinctStatus());
  }


  const onFinish = async (data) => {
    if (data?.invoMarkedPaidAmount == 0) {
      toast.error("Please add paid amount greater than zero");
      return;
    }
    const res = await dispatch(
      save({
        url: `api/v2/invoice/mark-as-paid/${prodd?.invoId}`,
        invoMarkedPaidValueDate: formatDate(data?.invoMarkedPaidValueDate),
        invoMarkedPaidReference: data?.invoMarkedPaidReference,
        invoStatus: data?.invoStatus,
        invoMarkedPaidAmount: removeCommas(formData1?.invoMarkedPaidAmount),
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await fetchInvoiceData();
      form.resetFields();
      await setIsModalOpen(false);
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  useEffect(()=>{
    fetchInvoiceDistinctData()
  },[])

  return (
    <>
      <Modal
        className=""
        title={title}
        open={isModalOpen}
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
          <Form.Item
            extra={"Choose date"}
            label={
              <span>
                Payment Date <span className="text-[#FF0000]">*</span>
              </span>
            } 
            name="invoMarkedPaidValueDate"
            rules={[
              {
                required: true,
                message: "Inoice Date is required",
              },
            ]}
            className="mr-6 w-full"
          >
            <DatePicker
              style={{
                width: "100%",
                height: "52px",
              }}
              placeholder="Select Date"
              className="mr-3 border border-black"
              format={"YYYY-MM-DD"}
            />
          </Form.Item>

          <Form.Item
            name="invoStatus"
            className=""
            label={
              <span>
                Select status <span className="text-[#FF0000]">*</span>
              </span>
            }
            rules={[
              {
                required: true,
                message: "Please select invoice status",
              },
            ]}
          >
            <Select
              className=""
              allowClear
              style={{
                width: "100%",
              }}
              onChange={(value) => {
                handleSelectChange(value, "invoStatus");
              }}
              options={
                invoDistinctStatus?.length > 0 &&
                invoDistinctStatus?.map((item) => ({
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
                fetchInvoiceDistinctData();
              }}
            />
          </Form.Item>

          <Form.Item
            extra={"Invoice description"}
            name="invoMarkedPaidReference"
            label={
              <span>
                Description of the Invoice
                <span className="text-[#FF0000]">*</span>
              </span>
            }
            rules={[
              {
                required: true,
                message: "Please add description of the description",
              },
            ]}
          >
            <TextArea  rows={4}  className="input-textarea"/>
          </Form.Item>

          <Form.Item
            extra={"Amount"}
            label={
              <span>
                Paid Amount<span className="text-[#FF0000]">*</span>
              </span>
            }
            rules={[
              {
                required: true,
                message: "Please add paid amount",
              },
            ]}
          >
            <Input
              name="invoMarkedPaidAmount"
              value={formData1?.invoMarkedPaidAmount}
              required
              min={0}
              type="text"
              onChange={onChange}
              className="input"
            />
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
export default AccpetanceModal;
