import React from "react";
import { Modal } from "antd";
import MaterialIcon from "material-icons-react";

// Branded invoice / receipt PDF previewer. Renders the already-fetched blob URL in an
// iframe and offers a download. The parent owns the blob URL lifecycle (create/revoke).
function InvoiceDocModal({ open, onClose, url, title, fileName }) {
  const handleDownload = () => {
    if (!url) return;
    const a = document.createElement("a");
    a.href = url;
    a.download = fileName || "document.pdf";
    document.body.appendChild(a);
    a.click();
    a.remove();
  };

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title={title}
      width={860}
      centered
      footer={[
        <button
          key="close"
          onClick={onClose}
          className="btn-ghost mr-2"
          type="button"
        >
          Close
        </button>,
        <button
          key="download"
          onClick={handleDownload}
          className="btn-primary inline-flex items-center gap-x-1"
          type="button"
          disabled={!url}
        >
          <MaterialIcon color="#ffffff" icon="download" size={18} />
          Download PDF
        </button>,
      ]}
    >
      {url ? (
        <iframe
          title={title}
          src={url}
          className="w-full rounded-[6px] border border-border"
          style={{ height: "70vh" }}
        />
      ) : (
        <div className="flex items-center justify-center" style={{ height: "70vh" }}>
          <span className="text-muted">Preparing document…</span>
        </div>
      )}
    </Modal>
  );
}

export default InvoiceDocModal;
