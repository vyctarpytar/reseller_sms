import { Form, InputNumber, Modal } from "antd";
import React from "react";
import toast from "react-hot-toast";
import { useDispatch, useSelector } from "react-redux";
import {
  buyUnitsFromWallet,
  fetchBills,
} from "../../../features/billing/billingSlice";

/**
 * Reseller buys SMS units from TOP using cash-wallet balance (no STK).
 * Posts to /api/v2/wallet/buy-units; the wallet is debited and units allocated server-side.
 */
function BuyUnitsModal({ open, onClose }) {
  const [form] = Form.useForm();
  const dispatch = useDispatch();
  const { buying } = useSelector((state) => state.billing);

  const handleFinish = async (values) => {
    if (!values.units || values.units <= 0) {
      toast.error("Enter the number of units to buy");
      return;
    }
    // Per-submit idempotency key so a network-level retry of the same purchase is a no-op server-side.
    const idempotencyKey =
      typeof crypto !== "undefined" && crypto.randomUUID
        ? crypto.randomUUID()
        : `buy-${Date.now()}-${Math.random().toString(36).slice(2)}`;
    const res = await dispatch(
      buyUnitsFromWallet({ units: values.units, idempotencyKey })
    );
    if (buyUnitsFromWallet.fulfilled.match(res)) {
      toast.success(res.payload?.messages?.message || "Units purchased");
      form.resetFields();
      dispatch(fetchBills());
      onClose?.();
    } else {
      toast.error(
        res.payload?.messages?.message ||
          res.payload?.message ||
          "Failed to buy units"
      );
    }
  };

  return (
    <Modal
      title="Buy SMS Units"
      open={open}
      onCancel={onClose}
      okText="Buy units"
      confirmLoading={buying}
      onOk={() => form.submit()}
    >
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item
          label="Number of units"
          name="units"
          rules={[{ required: true, message: "Enter number of units" }]}
        >
          <InputNumber
            min={1}
            className="!w-full"
            placeholder="e.g. 10000"
          />
        </Form.Item>
        <div className="text-[12px] text-[#666]">
          Units are bought from your provider at your unit price and paid from your
          M-Pesa wallet balance.
        </div>
      </Form>
    </Modal>
  );
}

export default BuyUnitsModal;
