import { Checkbox, Form, Input, Select, Spin, message } from "antd";
import React, { useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { IdTypeData, costData, ownershipData } from "../../data";
import Dragger from "antd/es/upload/Dragger";
import employeeTemplate from "../../assets/temp/Employee Returns Template.xlsx";
import ConfirmationModal from "../../components/ConfirmationModal";
import { save } from "../../features/save/saveSlice";
import toast from "react-hot-toast";
import { cleanRequestType } from "../../features/sms-request/smsRequestSlice";
import { fetchNewProductRequest } from "../../features/product-request/productRequestSlice";

const url = process.env.REACT_APP_API_BASE_URL;
const { TextArea } = Input;
function ShortCodeInfo({ next, prev }) {
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const linkRef = useRef(null);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const { saving } = useSelector((state) => state.save);

  const [isModalOpenConfirmation, setIsModalOpenConfirmation] = useState(false);
  const showModalConfirmation = async () => {
    setIsModalOpenConfirmation(true);
  };

  const [data, setData] = useState({});
  const plainOptions = ["Safaricom", "Airtel", "Telkom", "Equitel"];
  const [checkValues, setCheckValues] = useState("");

  function handleSelectChange(value, formName) {
    setData((prevData) => ({
      ...prevData,
      [formName]: value,
    }));
  }

  const onChangeCheckbox = (checkedValues) => {
    setCheckValues(checkedValues);
  };

  const handleBack = () => {
    prev();
  };

  async function clean(){
    await dispatch(cleanRequestType())
    }

  const onFinish = async (data) => {
    const res = await dispatch(
      save({
        url: "api/v2/req/short-code",
        telcos: checkValues?.join(","), 
        keyword: data?.keyword,
        desc: data?.desc,
      })
    );
    if (res?.payload?.success) {
      toast.success(res?.payload?.messages?.message);
      clean()
        fetchNewProductRequestData()
       await navigate('/code-request-list')
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };


  async function fetchNewProductRequestData() {
    const res = await dispatch(
      fetchNewProductRequest({
        reqStatus:"PENDING"
      })
    );
  }

  const handleClick = (event) => {
    event.preventDefault();
    if (linkRef.current) {
      linkRef.current.click();
    }
  };

  return (
    <>
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          marginBottom: "2.5rem",
          marginTop: "2.5rem",
        }}
      >
        <Form
          layout="vertical"
          ref={formRef}
          name="control-ref"
          onFinish={onFinish}
          style={{
            maxWidth: "100%",
          }}
          className="w-[850px]"
          form={form}
        >
          <div className="grid grid-cols-1 lg:px-10 px-3 py-10 border border-[#D4D4D8]">
            <div className="product_info_title">
              Two-Way SMS Shortcode Request
            </div>
            <Form.Item
              name="telcos"
              label={
                <span>
                  Telcos <span className="text-[#FF0000]">*</span>
                </span>
              }
              className="login-form-item "
              rules={[
                {
                  required: true,
                  message: "Please delect telcos",
                },
              ]}
            >
              <div
                className="w-full lg:h-[100px] h-auto border border-[#D4D4D8] rounded-[6px] flex 
              lg:flex-row flex-col lg:items-center justify-start lg:p-10 p-3"
              >
                <Checkbox.Group
                  className="flex lg:flex-row flex-col gap-y-2"
                  options={plainOptions}
                  onChange={onChangeCheckbox}
                />
              </div>
            </Form.Item>

          

            <Form.Item
              name="keyword"
              label={
                <span>
                  Keyword<span className="text-[#FF0000]">*</span>
                </span>
              }
              rules={[
                {
                  required: true,
                  message: "Please add keyword",
                },
              ]}
            >
              <Input className="input" />
            </Form.Item>

            <Form.Item
              name="desc"
              label={
                <span>
                  Description <span className="text-[#FF0000]">*</span>
                  Please describe in detail how you intend to use the service(s)
                  requested
                </span>
              }
              rules={[
                {
                  required: true,
                  message:
                    "Please describe in detail how you intend to use the service(s) requested",
                },
              ]}
            >
              <TextArea
                placeholder="Please describe in detail how you intend to use the service(s) requested"
                rows={4}
                className="input-textarea"
              />
            </Form.Item>

            <div className="mt-[20px]  lg:mb-0 mb-[20px] flex lg:px-0 px-3   justify-end  gap-x-4">
              <div className="w-[69px]">
                <button
                  type="button"
                  className={`cstm-btn !bg-[#fff] !text-[#374151] border border-[#D1D5DB]`}
                  onClick={showModalConfirmation}
                >
                  Back
                </button>
              </div>

              <div className="w-[109px]">
                <button
                  type="submit"
                  key="submit"
                  className={`cstm-btn`}
                  disabled={saving}
                >
                  {saving ? <Spin /> : "Finish"}
                </button>
              </div>
            </div>
          </div>
        </Form>
      </div>

      <ConfirmationModal
        isModalOpen={isModalOpenConfirmation}
        setIsModalOpen={setIsModalOpenConfirmation}
        handleBack={handleBack}
        content="Are you sure?"
        subContent="Are you sure that you want to leave this page? You will lose any unsaved changes."
      />
    </>
  );
}

export default ShortCodeInfo;
