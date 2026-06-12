import React, { useEffect, useRef, useState } from "react";
import { Form, Input, Modal, Spin, Select } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { fetchReseller } from "../../features/reseller/resellerSlice";
import { formatMoney, removeCommas, removeNegative } from "../../utils";
import { save } from "../../features/save/saveSlice";
import {
  fetchCreditAccount,
  fetchCreditAdmin,
  fetchCreditReseller,
} from "../../features/credit/creditSlice";
import numeral from "numeral";
import PhoneInput from "react-phone-input-2";
import { fetchResellerAccounts } from "../../features/reseller-account/resellerAccountSlice";
import { setResellerId } from "../../features/global/globalSlice";
import { fetchAccountBalance, fetchResellerBalance, fetchTopBalance } from "../../features/menu/menuSlice";

const CreditAddSelfModal = ({ isModalOpen, setIsModalOpen, prodd }) => {
  const handleOk = () => {
    setIsModalOpen(false);
  };
  const handleCancel = () => {
    setIsModalOpen(false);
  };
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const { user } = useSelector((state) => state.auth);
  const { resellerData, loading } = useSelector((state) => state.reseller);
  const { saving } = useSelector((state) => state.save);
  const { resellerId } = useSelector((state) => state.global);
  const { resellerAccountData } = useSelector((state) => state.resellerAccount);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  function closeModal() {
    toast.dismiss();
  }

  const [formData, setFormData] = useState({ ...prodd });

  function handleSelectChange(value, formName) {
    setFormData((prevData) => ({
      ...prevData,
      [formName]: value,
    }));
  }

  const [data, setdata] = useState({});

  useEffect(() => {
    setFormData(prodd);
  }, [prodd]);

  const onChange = (e) => {
    let { name, value } = e.target;
    const cleanedValue = value.replace(/[^0-9.]/g, "");
    const numericValue = Number(cleanedValue).toString();
    const formattedValue = numeral(numericValue).format("0,0");

    setFormData((prev) => ({
      ...prev,
      [name]: formattedValue,
    }));
  };

  const handleNumberChange = (e) => {
    setFormData((prevData) => ({
      ...prevData,
      smsPayerMobileNumber: String(e),
    }));
  };

  function fetchResellerData() {
    dispatch(fetchReseller());
  }

  async function fetchCreditData() {
    if (user?.layer === "ACCOUNT") {
      const res = await dispatch(fetchCreditAccount());
    }

    if (user?.layer === "RESELLER") {
      const res = await dispatch(fetchCreditReseller(fetchCreditAdmin()));
    }
    if (user?.layer === "TOP") {
      const res = await dispatch(fetchCreditAdmin());
    }
  } 
  const onFinish = async (data) => {
    if (!formData?.smsResellerId && user?.layer === "TOP") {
      toast.error("Please select a reseller");
      return;
    }
    if (formData?.smsPayAmount == 0) {
      toast.error("Please add sms amount greater than zero");
      return;
    }
    if (!formData?.smsPayerMobileNumber) {
      toast.error("Please add phone number");
      return;
    }
    // if(user?.layer === "RESELLER" && !resellerId){
    //   await toast.error("Please select  account");
    //   await navigate('/account-list')
    //   return;
    // }
    const res = await dispatch(
      save({
        // smsAccId: user?.layer === "RESELLER" ? formData?.smsAccId : "",
        // smsResellerId: user?.layer === "RESELLER" ? resellerId : "",
        // smsResellerId: user?.layer === "TOP" ? formData?.smsResellerId : null,
        smsPayAmount: removeCommas(formData?.smsPayAmount),
        smsPayerMobileNumber: formData?.smsPayerMobileNumber,
        url: "api/v2/credit/create-invoice",
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      form.resetFields();
      await dispatch(setResellerId(""));
      await fetchCreditData();
      await fetchBalanceData();
      await handleCancel();
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };


  async function fetchBalanceData() {
    if (user?.layer === "RESELLER") {
      dispatch(fetchResellerBalance());
    }
    if (user?.layer === "ACCOUNT") {
      dispatch(fetchAccountBalance());
    }
    if (user?.layer === "TOP") {
      dispatch(fetchTopBalance());
    }
  }


  // useEffect(() => {
  //   fetchResellerData();
  // }, []);

  function fetchResellerAccountData() {
    dispatch(fetchResellerAccounts());
  }

  // useEffect(() => {
  //   fetchResellerAccountData();
  // }, []);

  return (
    <>
      <Modal
        className=""
        title="Add Self Credit"
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
          <Form.Item
            label="Sms amount"
            rules={[
              {
                required: true,
                message: "Please add sms amount",
              },
            ]}
          >
            <Input
              name="smsPayAmount"
              value={formData?.smsPayAmount}
              required
              min={0}
              type="text"
              onChange={onChange}
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
                message: "Please add phone no",
              },
            ]}
          >
            <PhoneInput
              required
              name="smsPayerMobileNumber"
              value={formData?.smsPayerMobileNumber}
              onChange={handleNumberChange}
              country="ke"
              onlyCountries={["ke"]}
              countryCodeEditable={false}
              className="input rounded-[6px] border !border-[#cacaca] !h-[42px]"
            />
          </Form.Item>

          <div className="flex justify-between mt-[48px] ">
            <div className="justify-start"></div>
            <div className="justify-end flex items-center mb-[58px] gap-x-5">
              <div className="w-[150px] ">
                <button
                  key="back"
                  onClick={handleCancel}
                  className="cstm-btn !bg-white !text-[#388E3C] !border !border-[#388E3C]"
                >
                  Cancel
                </button>
              </div>

              <div className="w-[150px]">
                <button key="submit" type="submit" className="cstm-btn">
                  {saving ? <Spin /> : "Top Up"}
                </button>
              </div>
            </div>
          </div>
        </Form>
      </Modal>
    </>
  );
};
export default CreditAddSelfModal;
