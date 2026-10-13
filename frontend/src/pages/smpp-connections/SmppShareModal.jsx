import React from "react";
import { Modal } from "antd";
import toast from "react-hot-toast";
import {
  GATEWAY_SETTINGS,
  connectionCredentials,
  connectionDetailsText,
} from "./smppCredentials";

const Row = ({ label, value, mono }) => (
  <div className="flex gap-x-4 py-2 border-b border-[#f0f0f0] last:border-b-0">
    <div className="w-[160px] shrink-0 text-muted text-[13px]">{label}</div>
    <div className={`min-w-0 break-words text-[14px] ${mono ? "font-mono" : ""}`}>
      {value}
    </div>
  </div>
);

// Everything the other party needs to connect to us, on one sheet they can be
// sent as-is. Same idea as the API key's Dev SandBox page, for SMPP.
const SmppShareModal = ({ open, handleCancel, prodd }) => {
  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(connectionDetailsText(prodd));
      toast.success("Connection settings copied");
    } catch (e) {
      toast.error("Could not copy. Select the text and copy it manually.");
    }
  };

  return (
    <Modal
      title={`Connection settings${prodd?.accName ? ` - ${prodd.accName}` : ""}`}
      open={open}
      onCancel={handleCancel}
      footer={null}
      width={700}
    >
      <div className="lg:px-5 px-3">
        <p className="text-muted text-[13px] mb-4">
          Share this with the team connecting to us. It includes the password,
          so send it over a private channel.
        </p>

        <div className="rounded-[6px] border border-[#e2e8f0] bg-[#f8fafc] px-4 py-2">
          {connectionCredentials(prodd).map(([label, value]) => (
            <Row key={label} label={label} value={value} mono />
          ))}
        </div>

        <div className="mt-5 mb-2 font-medium">Settings</div>
        <div className="rounded-[6px] border border-[#e2e8f0] px-4 py-2">
          {GATEWAY_SETTINGS.map(([label, value]) => (
            <Row key={label} label={label} value={value} />
          ))}
        </div>

        <div className="flex justify-end items-center mt-[32px] mb-[40px] gap-x-5">
          <div className="w-[150px]">
            <button
              type="button"
              onClick={handleCancel}
              className="cstm-btn !bg-white !text-[var(--brand)] !border !border-[var(--brand)]"
            >
              Close
            </button>
          </div>
          <div className="w-[200px]">
            <button type="button" className="cstm-btn" onClick={handleCopy}>
              Copy all
            </button>
          </div>
        </div>
      </div>
    </Modal>
  );
};

export default SmppShareModal;
