import React, { useEffect } from "react";
import { Form, Input, InputNumber, Modal, Select, Spin } from "antd";
import { useDispatch, useSelector } from "react-redux";
import toast from "react-hot-toast";
import { save } from "../../features/save/saveSlice";
import { fetchSmppConnections } from "../../features/smpp/smppSlice";
import { fetchResellerAccounts } from "../../features/reseller-account/resellerAccountSlice";
import {
  BIND_TYPES,
  CONNECTION_TYPES,
  PASSWORD_MAX,
  PASSWORD_PATTERN,
  SYSTEM_ID_MAX,
  SYSTEM_ID_PATTERN,
  UPSTREAM_PASSWORD_PATTERN,
  UPSTREAM_SYSTEM_ID_PATTERN,
  generatePassword,
  suggestSystemId,
} from "./smppCredentials";

const SmppConnectionModal = ({ isModalOpen, setIsModalOpen, prodd }) => {
  const [form] = Form.useForm();
  const dispatch = useDispatch();
  const { user } = useSelector((state) => state.auth);
  const { saving } = useSelector((state) => state.save);
  const { smppConnections } = useSelector((state) => state.smpp);
  const { resellerAccountData } = useSelector((state) => state.resellerAccount);

  const isEdit = !!prodd?.id;
  // CLIENT = we bind out to an upstream SMSC with credentials THEY issued, so
  // nothing is generated and the account is optional. TOP only.
  const isTop = user?.layer === "TOP";
  const connectionType = Form.useWatch("connectionType", form) ?? "SERVER";
  const isUpstream = connectionType === "CLIENT";

  const accountOptions = (resellerAccountData ?? [])
    .filter((acc) => acc?.accStatus !== "DELETED")
    .map((acc) => ({ value: acc?.accId, label: acc?.accName }));

  const handleCancel = () => {
    setIsModalOpen(false);
  };

  // Picking the account fills in a username and password derived from it; both
  // stay editable. Only on create — an issued username can't be changed.
  const handleAccountChange = (accId) => {
    if (isEdit || isUpstream) return;
    const acc = resellerAccountData?.find((a) => a?.accId === accId);
    form.setFieldsValue({
      systemId: suggestSystemId(
        acc?.accName,
        (smppConnections ?? []).map((c) => c?.systemId)
      ),
      password: generatePassword(),
    });
  };

  const onFinish = async (data) => {
    const res = await dispatch(
      save({
        url: "api/v2/smpp-connection",
        ...data,
        id: prodd?.id,
      })
    );
    if (res?.payload?.success) {
      toast.success(res?.payload?.messages?.message);
      await dispatch(fetchSmppConnections());
      // The account's SMPP flag may have just flipped.
      dispatch(fetchResellerAccounts());
      form.resetFields();
      setIsModalOpen(false);
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  useEffect(() => {
    if (!isModalOpen) return;
    if (isEdit) {
      form.setFieldsValue(prodd);
    } else {
      form.resetFields();
      form.setFieldsValue({ bindType: "TRANSCEIVER", connectionType: "SERVER" });
    }
  }, [isModalOpen, prodd]);

  return (
    <Modal
      title={`${isEdit ? "Update" : "Add"} ${isUpstream ? "upstream SMPP connection" : "SMPP connection"}`}
      open={isModalOpen}
      onCancel={handleCancel}
      footer={null}
      width={700}
    >
      <div className="lg:px-5 px-3">
        <Form layout="vertical" onFinish={onFinish} form={form}>
          {isTop ? (
            <Form.Item
              label="Direction"
              name="connectionType"
              rules={[{ required: true, message: "Required field" }]}
            >
              <Select
                disabled={isEdit}
                options={CONNECTION_TYPES}
                onChange={() =>
                  form.setFieldsValue({ systemId: undefined, password: undefined })
                }
              />
            </Form.Item>
          ) : (
            <Form.Item name="connectionType" hidden>
              <Input />
            </Form.Item>
          )}

          <Form.Item
            label={isUpstream ? "Account (optional)" : "Account"}
            name="accId"
            rules={[{ required: !isUpstream, message: "Required field" }]}
            extra={
              isUpstream
                ? "Leave empty to use this upstream for every account of this reseller."
                : "Messages sent over this connection are billed to this account."
            }
          >
            <Select
              showSearch
              allowClear={isUpstream}
              optionFilterProp="label"
              placeholder={isUpstream ? "All accounts of this reseller" : "Select account"}
              disabled={isEdit}
              options={accountOptions}
              onChange={handleAccountChange}
            />
          </Form.Item>

          {isUpstream && (
            <div className="flex gap-x-5">
              <Form.Item
                label="Upstream host"
                name="host"
                rules={[{ required: true, message: "Required field" }]}
                className="flex-1"
              >
                <Input className="input" maxLength={100} placeholder="IP address or hostname" />
              </Form.Item>
              <Form.Item
                label="Port"
                name="port"
                rules={[{ required: true, message: "Required field" }]}
                className="w-[140px]"
              >
                <InputNumber className="input !w-full" min={1} max={65535} controls={false} />
              </Form.Item>
            </div>
          )}

          <Form.Item
            label="Username (system ID)"
            name="systemId"
            rules={[
              { required: true, message: "Required field" },
              isUpstream
                ? {
                    pattern: UPSTREAM_SYSTEM_ID_PATTERN,
                    message: `Up to ${SYSTEM_ID_MAX} characters, no spaces`,
                  }
                : {
                    pattern: SYSTEM_ID_PATTERN,
                    message: `3-${SYSTEM_ID_MAX} characters: letters, digits, _ - or .`,
                  },
            ]}
            extra={
              isEdit
                ? "Cannot be changed once saved. Deactivate this connection and add a new one instead."
                : isUpstream
                ? "As issued by the upstream provider."
                : "Generated from the account name. You can change it."
            }
          >
            <Input className="input" maxLength={SYSTEM_ID_MAX} disabled={isEdit} />
          </Form.Item>

          <Form.Item
            label="Password"
            name="password"
            rules={[
              { required: true, message: "Required field" },
              isUpstream
                ? {
                    pattern: UPSTREAM_PASSWORD_PATTERN,
                    message: `Up to ${PASSWORD_MAX} characters, no spaces`,
                  }
                : {
                    pattern: PASSWORD_PATTERN,
                    message: `6-${PASSWORD_MAX} characters, no spaces (SMPP allows at most ${PASSWORD_MAX})`,
                  },
            ]}
            extra={
              isUpstream ? (
                "As issued by the upstream provider."
              ) : (
              <button
                type="button"
                className="text-primary"
                onClick={() => form.setFieldsValue({ password: generatePassword() })}
              >
                Generate a new password
              </button>
              )
            }
          >
            <Input className="input" maxLength={PASSWORD_MAX} />
          </Form.Item>

          <Form.Item
            label="Bind mode"
            name="bindType"
            rules={[{ required: true, message: "Required field" }]}
          >
            <Select options={BIND_TYPES} />
          </Form.Item>

          {isUpstream && (
            <Form.Item
              label="Priority"
              name="priority"
              extra="When several upstreams could carry a message, the lowest number is used first."
            >
              <InputNumber className="input !w-full" min={1} controls={false} placeholder="1" />
            </Form.Item>
          )}

          <Form.Item label="Label (optional)" name="connectionName">
            <Input className="input" maxLength={100} placeholder="e.g. Production" />
          </Form.Item>

          <div className="flex justify-end items-center mt-[48px] mb-[58px] gap-x-5">
            <div className="w-[150px]">
              <button
                type="button"
                onClick={handleCancel}
                className="cstm-btn !bg-white !text-[var(--brand)] !border !border-[var(--brand)]"
              >
                Cancel
              </button>
            </div>
            <div className="w-[200px]">
              <button type="submit" className="cstm-btn" disabled={saving}>
                {saving ? <Spin /> : isEdit ? "Update" : "Create"}
              </button>
            </div>
          </div>
        </Form>
      </div>
    </Modal>
  );
};

export default SmppConnectionModal;
