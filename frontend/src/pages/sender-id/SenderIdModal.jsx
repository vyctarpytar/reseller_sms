import React, { useEffect, useRef, useState } from "react";
import { 
  Form, 
  Modal,
  Spin,
  Select, 
  Space,
  Divider,
  Checkbox,
} from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom"; 
import toast from "react-hot-toast"; 
import {  fetchSenderIds, save } from "../../features/save/saveSlice"; 
import { fetchResellerAccounts } from "../../features/reseller-account/resellerAccountSlice";
import { fetchSenderId } from "../../features/sms-request/smsRequestSlice";

const SenderIdModal = ({ isModalOpen, setIsModalOpen, prodd }) => {
 
  const handleCancel = () => {
    setIsModalOpen(false);
  };
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const { user } = useSelector(
    (state) => state.auth
  );
  const { saving,senderIdData } = useSelector((state) => state.save);
  const { resellerAccountData, loading } = useSelector(
    (state) => state.resellerAccount
  );
 
  

  const dispatch = useDispatch();
  const navigate = useNavigate();

  function closeModal() {
    toast.dismiss();
  }

 
  const [data, setdata] = useState({});
 
  function fetchResellerAccountData() {
    dispatch(fetchResellerAccounts());
  }

  const onFinish = async (data) => {
    // if(initialSelectedAccounts?.length === 0){
    //   toast.error("Select atleast one sender ID")
    //   return;
    // }  
    
    const res = await dispatch(
      save({
        url: `api/v2/shortcode/assign/${prodd?.accId}`,
        shIds: initialSelectedAccounts?.join(","),
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await fetchResellerAccountData();
      await form.resetFields();
      await setIsModalOpen(false);
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  function fetchResellerSenderIdData() {
    dispatch(fetchSenderIds({
      url:'api/v2/shortcode',
      limit:1000000
    })) 
  }
 

  const [initialSelectedAccounts, setInitialSelectedAccounts] = useState([]);  
 
  
  useEffect(() => {
    if (prodd?.senderId) { 
      const shIds = prodd?.senderId.map((acc) => acc?.shCode);  
      setInitialSelectedAccounts(shIds);   
      form.setFieldsValue({ shId: shIds });
    }else{ 
      setInitialSelectedAccounts([]); 
      form.resetFields(["shId"]);
    }
  }, [prodd,form]);
  const handleSelectChange = (value, formName) => {
    setInitialSelectedAccounts(value);
    form.setFieldsValue({ [formName]: value });
  };
 
 

  return (
    <>
      <Modal
        className=""
        title={`Assign ${prodd?.accName} sender IDs`}
        open={isModalOpen}
        onOk={onFinish}
        onCancel={handleCancel}
        width={800}
      >
        <div className="lg:px-5 px-3">
        
          <Form
            layout="vertical"
            ref={formRef}
            name="control-ref"
            onFinish={onFinish}
            className=""
            style={{
              maxWidth: "100%",
            }}
            form={form} 
          >
            <Form.Item
              extra={"You can choose more than one sender ID"}
              label="Select sender ID"
              rules={[
                {
                  required: true,
                  message: "Required field",
                },
              ]}
              className="w-full"
            >
              <Select
              value={initialSelectedAccounts}
              name="shId"
              onChange={(value) => {
                handleSelectChange(value, "shId");
              }}
                mode="multiple"
                allowClear
                style={{
                  minHeight: "52px",
                }}
                placeholder={"Select sender ID"}
                size="large"
                className=""
                showSearch
                optionFilterProp="children"
                filterOption={(input, option) =>
                  (option?.children?.toLowerCase() ?? "").includes(
                    input.toLowerCase()
                  )
                }
                onDropdownVisibleChange={(open) => {
                  if (open) {
                    fetchResellerSenderIdData();
                  }
                }}
                dropdownRender={(menu) => (
                  <>
                    <Space
                      style={{
                        padding: "8px 8px 4px",
                      }}
                    >
                      <span className="select-info mt-1">
                        Choose one or more of the following
                      </span>
                    </Space>
                    <Divider
                      style={{
                        margin: "8px 0",
                      }}
                    />
                    {menu}
                  </>
                )}
                menuItemSelectedIcon={({ isSelected }) => (
                  <>
                    {isSelected ? (
                      <Checkbox checked={true}></Checkbox>
                    ) : (
                      <Checkbox checked={false}></Checkbox>
                    )}
                  </>
                )}
              >
                {senderIdData?.map((option) => (
                  <Select.Option key={option?.shId} value={option?.shCode}>
                    {option?.shCode}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

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

                <div className="w-[200px]">
                  <button key="submit" type="submit" className="cstm-btn">
                    {saving ? <Spin /> : "Assign Account"}
                  </button>
                </div>
              </div>
            </div>
          </Form>
        </div>
      </Modal>
    </>
  );
};
export default SenderIdModal;
