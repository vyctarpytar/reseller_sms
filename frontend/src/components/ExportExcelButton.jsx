import React from "react";
import { Tooltip, Spin } from "antd";
import MaterialIcon from "material-icons-react";

// Shared "Export to Excel" button so every screen (Sent SMS, Credits, Reports)
// renders the same brand-styled control. Source of truth = the Sent SMS screen.
function ExportExcelButton({ onClick, loading = false, label = "Export to Excel" }) {
  if (loading) return <Spin className="sms-spin" />;
  return (
    <Tooltip placement="top" title="Download Excel">
      <button
        type="button"
        onClick={onClick}
        className="flex items-center gap-x-2 rounded-lg border border-[#E5E0DA] bg-white px-4 py-2 text-[14px] font-medium text-[#5A4632] hover:bg-[#F7F5F2] hover:border-[#69472E] transition-colors"
      >
        <MaterialIcon size={20} color="#69472E" icon="file_download" />
        <span>{label}</span>
      </button>
    </Tooltip>
  );
}

export default ExportExcelButton;
