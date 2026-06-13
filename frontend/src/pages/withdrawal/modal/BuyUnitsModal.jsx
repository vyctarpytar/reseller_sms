import { InputNumber, Modal, Segmented } from "antd";
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
 *
 * The caller enters EITHER a unit count or a KES amount (toggle); the other side is derived
 * from the reseller unit price for preview, and the server derives it again authoritatively.
 * Two steps: enter → confirm.
 *
 * The entered value lives in plain React state (not Form.useWatch): the confirm step unmounts
 * the input, and a watched-field value collapses to 0 once its field unmounts — which is why a
 * typed "13" used to reach the API as units:0.
 */
function BuyUnitsModal({ open, onClose }) {
  const dispatch = useDispatch();
  const { buying, billsData } = useSelector((state) => state.billing);

  const [step, setStep] = useState("form"); // "form" | "confirm"
  const [mode, setMode] = useState("units"); // "units" | "amount"
  const [value, setValue] = useState(null); // raw input for the active mode

  const wallet = billsData?.[0] || {};
  const walAmount = Number(wallet?.walAmount) || 0;
  const unitPrice = Number(wallet?.walUnitPrice) || 0;
  const priceReady = unitPrice > 0;
  const maxUnits = priceReady ? Math.floor(walAmount / unitPrice) : 0;

  // Resolve the units / cash-cost pair from whichever side is being entered. The typed side is
  // exact; the other is derived from the unit price (null when the price isn't configured yet —
  // the server still computes it at purchase).
  const entered = Number(value) || 0;
  const units =
    mode === "units"
      ? Math.floor(entered)
      : priceReady
      ? Math.floor(entered / unitPrice)
      : null;
  const cost =
    mode === "amount" ? entered : priceReady ? entered * unitPrice : null;
  const exceedsBalance = cost != null && cost > walAmount;

  // Start from a clean form on a fresh balance whenever the modal opens.
  useEffect(() => {
    if (open) {
      setStep("form");
      setMode("units");
      setValue(null);
      dispatch(fetchBills());
    }
  }, [open]);

  // Switching unit/amount mode clears the field so the two never get crossed.
  const handleModeChange = (next) => {
    setMode(next);
    setValue(null);
  };

  const handleContinue = () => {
    if (!entered || entered <= 0) {
      toast.error(
        mode === "units"
          ? "Enter the number of units to buy"
          : "Enter an amount to spend"
      );
      return;
    }
    if (exceedsBalance) {
      toast.error("That exceeds your wallet balance");
      return;
    }
    if (mode === "amount" && priceReady && units < 1) {
      toast.error(`Enter at least ${cashConverter(unitPrice)} to buy a unit`);
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
    // Send whichever side the caller entered; the server derives the rest from the unit price.
    const payload =
      mode === "units"
        ? { units, idempotencyKey }
        : { amount: entered, idempotencyKey };
    const res = await dispatch(buyUnitsFromWallet(payload));
    if (buyUnitsFromWallet.fulfilled.match(res)) {
      toast.success(res.payload?.messages?.message || "Units purchased");
      setValue(null);
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

  const continueDisabled =
    !entered ||
    exceedsBalance ||
    (mode === "amount" && priceReady && units < 1);

  return (
    <Modal
      title={step === "confirm" ? "Confirm purchase" : "Buy SMS Units"}
      open={open}
      onCancel={onClose}
      footer={null}
    >
      {step === "form" ? (
        <div>
          <div className="flex items-center justify-between bg-[#f6f6f6] rounded-[8px] px-3 py-2 mb-4">
            <span className="text-[12px] text-[#666]">Wallet balance</span>
            <span className="text-[14px] font-[700] text-[#222]">
              {cashConverter(walAmount)}
            </span>
          </div>

          <Segmented
            block
            value={mode}
            onChange={handleModeChange}
            options={[
              { label: "By units", value: "units" },
              { label: "By amount", value: "amount" },
            ]}
            className="mb-4"
          />

          <label className="block text-[13px] text-[#333] font-[500] mb-1">
            {mode === "units" ? "Number of units" : "Amount to spend (KES)"}
          </label>
          <InputNumber
            autoFocus
            min={mode === "units" ? 1 : 0.01}
            max={
              mode === "units"
                ? priceReady
                  ? maxUnits
                  : undefined
                : walAmount || undefined
            }
            precision={mode === "units" ? 0 : 2}
            value={value}
            onChange={setValue}
            className="!w-full"
            placeholder={
              mode === "units"
                ? priceReady
                  ? `Up to ${numberWithCommas(maxUnits)} units`
                  : "e.g. 10000"
                : `Up to ${cashConverter(walAmount)}`
            }
          />

          {priceReady ? (
            <div className="flex items-center justify-between mt-2">
              <div className="text-[12px] text-[#666]">
                {mode === "units" ? (
                  <>
                    Unit price {cashConverter(unitPrice)} · max{" "}
                    {numberWithCommas(maxUnits)} units
                  </>
                ) : (
                  <>Unit price {cashConverter(unitPrice)}</>
                )}
              </div>
              <div
                className={`text-[13px] font-[600] ${
                  exceedsBalance ? "text-red" : "text-[#222]"
                }`}
              >
                {mode === "units"
                  ? cashConverter(cost || 0)
                  : `${numberWithCommas(units || 0)} units`}
              </div>
            </div>
          ) : (
            <div className="text-[12px] text-red mt-2">
              Your unit price isn't configured yet — contact your provider.
            </div>
          )}
        </div>
      ) : (
        <div>
          <div className="text-[13px] text-[#666] mb-3">
            You're buying SMS units from your M-Pesa wallet balance.
          </div>
          <div className="rounded-[10px] border border-[#eee] p-3">
            <Row
              label="Units"
              value={units != null ? numberWithCommas(units) : "Calculated at purchase"}
            />
            <Row
              label="Unit price"
              value={priceReady ? cashConverter(unitPrice) : "—"}
            />
            <Row
              label="Total cost"
              value={cost != null ? cashConverter(cost) : "Calculated at purchase"}
              strong
            />
            <div className="border-t border-[#eee] my-1" />
            <Row label="Wallet balance" value={cashConverter(walAmount)} />
            {cost != null && (
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
              disabled={continueDisabled}
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
