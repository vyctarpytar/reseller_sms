import { Form, InputNumber, Modal } from "antd";
import React, { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { useDispatch, useSelector } from "react-redux";
import {
  buyUnitsFromWallet,
  fetchBills,
} from "../../../features/billing/billingSlice";
import { cashConverter, numberWithCommas } from "../../../utils";

/**
 * Reseller buys SMS units from TOP using cash-wallet balance (no STK).
 * Posts to /api/v2/wallet/buy-units; the wallet is debited and units allocated server-side.
 * Two steps: enter units (capped at what the wallet balance affords) → confirm summary.
 */
function BuyUnitsModal({ open, onClose }) {
  const [form] = Form.useForm();
  const dispatch = useDispatch();
  const { buying, billsData } = useSelector((state) => state.billing);

  const [step, setStep] = useState("form");
  const units = Form.useWatch("units", form) || 0;

  const wallet = billsData?.[0] || {};
  const walAmount = Number(wallet?.walAmount) || 0;
  const unitPrice = Number(wallet?.walUnitPrice) || 0;
  const maxUnits = unitPrice > 0 ? Math.floor(walAmount / unitPrice) : 0;
  const cost = unitPrice > 0 ? units * unitPrice : 0;
  const exceedsBalance = cost > walAmount;
  const priceReady = unitPrice > 0;

  // Always start from a clean form on a fresh balance whenever the modal opens.
  useEffect(() => {
    if (open) {
      setStep("form");
      form.resetFields();
      dispatch(fetchBills());
    }
  }, [open]);

  const handleContinue = async () => {
    try {
      await form.validateFields();
    } catch {
      return;
    }
    if (!units || units <= 0) {
      toast.error("Enter the number of units to buy");
      return;
    }
    // When the price is known we cap locally; when it isn't, the server still
    // rejects an over-balance purchase, so we let the confirm step proceed.
    if (priceReady && exceedsBalance) {
      toast.error("Cost exceeds your wallet balance");
      return;
    }
    setStep("confirm");
  };

  const handleConfirm = async () => {
    // Per-submit idempotency key so a network-level retry of the same purchase is a no-op server-side.
    const idempotencyKey =
      typeof crypto !== "undefined" && crypto.randomUUID
        ? crypto.randomUUID()
        : `buy-${Date.now()}-${Math.random().toString(36).slice(2)}`;
    const res = await dispatch(
      buyUnitsFromWallet({ units, idempotencyKey })
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

  const Row = ({ label, value, strong }) => (
    <div className="flex items-center justify-between py-1.5">
      <span className="text-[13px] text-[#666]">{label}</span>
      <span
        className={`text-[13px] ${
          strong ? "font-[700] text-[#222]" : "font-[500] text-[#333]"
        }`}
      >
        {value}
      </span>
    </div>
  );

  return (
    <Modal
      title={step === "confirm" ? "Confirm purchase" : "Buy SMS Units"}
      open={open}
      onCancel={onClose}
      footer={null}
    >
      {step === "form" ? (
        <Form form={form} layout="vertical">
          <div className="flex items-center justify-between bg-[#f6f6f6] rounded-[8px] px-3 py-2 mb-4">
            <span className="text-[12px] text-[#666]">Wallet balance</span>
            <span className="text-[14px] font-[700] text-[#222]">
              {cashConverter(walAmount)}
            </span>
          </div>

          <Form.Item
            label="Number of units"
            name="units"
            rules={[
              { required: true, message: "Enter number of units" },
              () => ({
                validator(_, value) {
                  if (!value || value <= 0) return Promise.resolve();
                  if (priceReady && value * unitPrice > walAmount) {
                    return Promise.reject(
                      new Error(
                        `Max ${numberWithCommas(maxUnits)} units for your balance`
                      )
                    );
                  }
                  return Promise.resolve();
                },
              }),
            ]}
          >
            <InputNumber
              min={1}
              max={priceReady ? maxUnits : undefined}
              className="!w-full"
              placeholder={
                priceReady ? `Up to ${numberWithCommas(maxUnits)} units` : "e.g. 10000"
              }
            />
          </Form.Item>

          {priceReady ? (
            <div className="flex items-center justify-between">
              <div className="text-[12px] text-[#666]">
                Unit price {cashConverter(unitPrice)} · max{" "}
                {numberWithCommas(maxUnits)} units
              </div>
              <div
                className={`text-[13px] font-[600] ${
                  exceedsBalance ? "text-red" : "text-[#222]"
                }`}
              >
                {cashConverter(cost)}
              </div>
            </div>
          ) : (
            <div className="text-[12px] text-red">
              Your unit price isn't configured yet — contact your provider.
            </div>
          )}
        </Form>
      ) : (
        <div>
          <div className="text-[13px] text-[#666] mb-3">
            You're buying SMS units from your M-Pesa wallet balance.
          </div>
          <div className="rounded-[10px] border border-[#eee] p-3">
            <Row label="Units" value={numberWithCommas(units)} />
            <Row
              label="Unit price"
              value={priceReady ? cashConverter(unitPrice) : "—"}
            />
            <Row
              label="Total cost"
              value={priceReady ? cashConverter(cost) : "Calculated at purchase"}
              strong
            />
            <div className="border-t border-[#eee] my-1" />
            <Row label="Wallet balance" value={cashConverter(walAmount)} />
            {priceReady && (
              <Row
                label="Balance after"
                value={cashConverter(walAmount - cost)}
              />
            )}
          </div>
        </div>
      )}

      {/* Action buttons live inside the modal body because the global
          `.ant-modal-footer { display:none }` rule (src/antd.css) hides the
          native antd footer for every modal in this app. */}
      <div className="flex items-center justify-end mt-5">
        {step === "confirm" ? (
          <>
            <button
              type="button"
              onClick={() => setStep("form")}
              className="cstm-btn !bg-white !text-[var(--brand)] !border !border-[var(--brand)] !w-auto px-5 mr-2"
            >
              Back
            </button>
            <button
              type="button"
              disabled={buying}
              onClick={handleConfirm}
              className="cstm-btn !w-auto px-5"
            >
              {buying ? "Processing..." : "Confirm purchase"}
            </button>
          </>
        ) : (
          <>
            <button
              type="button"
              onClick={onClose}
              className="cstm-btn !bg-white !text-[var(--brand)] !border !border-[var(--brand)] !w-auto px-5 mr-2"
            >
              Cancel
            </button>
            <button
              type="button"
              disabled={!units || (priceReady && exceedsBalance)}
              onClick={handleContinue}
              className="cstm-btn !w-auto px-5"
            >
              Continue
            </button>
          </>
        )}
      </div>
    </Modal>
  );
}

export default BuyUnitsModal;
