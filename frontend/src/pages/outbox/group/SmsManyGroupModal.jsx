import React, { useEffect, useRef, useState } from "react";
import { Button, Form, Input, Modal, Select, Spin, Tag, Tooltip } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { save } from "../../../features/save/saveSlice";
import MessageTemplate from "../../mesage-template/MessageTemplate";
import svg7 from "../../../assets/svg/svg7.svg";
import svg43 from "../../../assets/svg/svg43.svg";
import { fetchDistinctSenderNames } from "../../../features/filter/filterSlice";
import ScheduleManyGroupsModal from "./modal/ScheduleManyGroupsModal";

const { TextArea } = Input;
const SmsManyGroupModal = ({
  isModalOpen,
  setIsModalOpen,
  rowId,
  rowIdTableGroup,
}) => {
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
  const { folderObj } = useSelector((state) => state.folder);
  const { senderNamesData } = useSelector((state) => state.filterSms);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  function closeModal() {
    toast.dismiss();
  }

  const [data, setdata] = useState({});
  const [inputValue, setInputValue] = useState(
    rowId?.length > 0 ? rowId?.[0]?.tmpMessage : ""
  );

  const [isModalSchedule, setIsModalSchedule] = useState(false);
  const showModalConfirmation = async () => {
    if (!inputValue) {
      toast.error("Please enter message");
      return;
    }
    setIsModalSchedule(true);
  };

  const [formData, setFormData] = useState();
  async function handleSelectChange(value, formName) {
    await setFormData((prevData) => ({
      ...prevData,
      [formName]: value,
    }));
  }
 
  const onFinish = async (data) => {
    if (!inputValue) {
      toast.error("Please add message");
      return;
    }
    const res = await dispatch(
      save({
        url: `api/v2/sms/multi-group`,
        grpMessage: inputValue,
        senderId: formData?.senderId,
        grpIds: rowIdTableGroup?.map((item) => item?.groupId).join(","),
      })
    );
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      await form.resetFields();
      await setInputValue("");
      await setIsModalOpen(false);
      await navigate("/sent-sms");
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  function fetchDistinctSenderNamesData() {
    dispatch(fetchDistinctSenderNames());
  }

  useEffect(() => {
    setInputValue(rowId?.length > 0 ? rowId?.[0]?.tmpMessage : "");
  }, [rowId]);

  useEffect(() => {
    fetchDistinctSenderNamesData();
  }, []);

 
  return (
    <>
      <Modal
        className=""
        title="Message"
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
          {rowIdTableGroup && rowIdTableGroup?.length > 0 && (
            <div className="text-dmSans text-[16px] mb-10 flex flex-col">
              Selected Groups:
              {rowIdTableGroup &&
                rowIdTableGroup?.map((item) => (
                  <span className=" ">
                    <Tag className="bg-green rounded-full px-3 py-1 text-[16px] font-dmSans text-white">
                      {item?.groupName}
                    </Tag>
                  </span>
                ))}
            </div>
          )}

          <Form.Item
            name="senderId"
            className=""
            label={
              <span>
                Select sender name <span className="text-[#FF0000]">*</span>
              </span>
            }
            rules={[
              {
                required: true,
                message: "Please select sender name",
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
                handleSelectChange(value, "senderId");
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
                fetchDistinctSenderNamesData();
              }}
            />
          </Form.Item>

          <MessageTemplate
            inputValue={inputValue}
            setInputValue={setInputValue}
          />

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

              <div className="flex">
                <div className="w-[150px]">
                  <button
                    key="submit"
                    type="submit"
                    className="cstm-btn  !rounded-tr-[0px] !rounded-br-[0px]"
                    disabled={saving}
                  >
                    {saving ? <Spin /> : "Send"}
                  </button>
                </div>
                <div className="w-[65px]">
                  <Button
                    onClick={showModalConfirmation}
                    className="cstm-btn   !bg-[#739675] !rounded-tl-[0px] !rounded-bl-[0px]  !h-[2.6rem] "
                  >
                    {saving ? (
                      <Spin />
                    ) : (
                      <Tooltip title="Schedule send">
                        <img className="" src={svg43} alt="svg43" />
                      </Tooltip>
                    )}
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </Form>
      </Modal>

      <ScheduleManyGroupsModal
        isModalOpen={isModalSchedule}
        setIsModalOpen={setIsModalSchedule}
        title={`Schedule Send`}
        inputValue={inputValue}
        setInputValue={setInputValue}
        setIsModalOpenPrev={setIsModalOpen}
        grpIds = {rowIdTableGroup?.map((item) => item?.groupId)?.join(",")}
        senderId={formData?.senderId}
      />
    </>
  );
};
export default SmsManyGroupModal;
