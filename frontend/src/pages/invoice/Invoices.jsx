import { Skeleton, Spin } from "antd";
import React, { useEffect, useState } from "react";
import InsideHeader from "../../components/InsideHeader";
import ResponsiveTable, { hideBelow } from "../../components/ResponsiveTable";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import MaterialIcon from "material-icons-react";
import { addSpaces, cashConverter, customToast, dateForHumans } from "../../utils";
import svg38 from "../../assets/svg/svg38.svg";
import FilterModal from "./FilterModal";
import {
  fetchInvoices,
  fetchInvoiceDocument,
} from "../../features/invoice/invoiceSlice";
import StatusBadge from "../../components/StatusBadge";
import InvoiceDocModal from "./InvoiceDocModal";

function Invoices() {
  const { invoiceData, invoiceCount, loading } = useSelector(
    (state) => state.inv
  );

  const navigate = useNavigate();
  const dispatch = useDispatch();

  const [formData, setFormData] = useState({});

  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };

  // Branded invoice / receipt preview + download
  const [docLoadingKey, setDocLoadingKey] = useState(null);
  const [docModal, setDocModal] = useState({
    open: false,
    url: null,
    title: "",
    fileName: "",
  });

  const openDoc = async (record) => {
    setDocLoadingKey(`${record?.invoId}-invoice`);
    try {
      const res = await dispatch(
        fetchInvoiceDocument({ invoId: record?.invoId, type: "invoice" })
      );
      if (res?.payload instanceof Blob) {
        if (docModal.url) URL.revokeObjectURL(docModal.url);
        setDocModal({
          open: true,
          url: URL.createObjectURL(res.payload),
          title: `Invoice · ${record?.invoCode ?? ""}`,
          fileName: `invoice-${record?.invoCode ?? record?.invoId}.pdf`,
        });
      } else {
        customToast({
          content: "Could not generate the document. Please try again.",
          bdColor: "error",
        });
      }
    } finally {
      setDocLoadingKey(null);
    }
  };

  const closeDoc = () => {
    if (docModal.url) URL.revokeObjectURL(docModal.url);
    setDocModal({ open: false, url: null, title: "", fileName: "" });
  };

  const columns = [
    {
      title: "Code",
      dataIndex: "invoCode",
    },
    {
      title: "Amount",
      dataIndex: "invoAmount",
      render: (value) => <div>{cashConverter(value)}</div>,
    },
    {
      title: "Pay Mobile",
      ...hideBelow(),
      render: (item) => {
        return <div>{addSpaces(item)}</div>;
      },
      dataIndex: "invoPayerMobileNumber",
    },
    {
      title: "Created By Email",
      ...hideBelow(),
      dataIndex: "invoCreatedByEmail",
    },
    {
      title: "Created Date",
      ...hideBelow(),
      dataIndex: "invoCreatedDate",
      render: (value) => <div>{dateForHumans(value)}</div>,
    },
    {
      title: "Due Date",
      ...hideBelow(),
      dataIndex: "invoDueDate",
      render: (value) => <div>{dateForHumans(value)}</div>,
    },
    {
      title: "Status",
      align: "center",
      render: (_, record) => (
        <div className="flex items-center justify-center">
          <StatusBadge value={record?.invoStatus} />
        </div>
      ),
    },
    {
      title: "Document",
      align: "center",
      render: (_, record) => {
        const busy =
          docLoadingKey && docLoadingKey.startsWith(`${record?.invoId}-`);
        return (
          <button
            type="button"
            className="btn-ghost px-3 py-1"
            disabled={!!busy}
            onClick={() => openDoc(record)}
          >
            {busy ? (
              <Spin size="small" />
            ) : (
              <MaterialIcon icon="picture_as_pdf" color="#69472E" />
            )}
          </button>
        );
      },
    },
  ];

  const handleSendSms = () => {
    navigate("/outbox");
  };

  const handleClearFilters = async (page, size) => {
    await setFormData({});
    await dispatch(
      fetchInvoices({
        url: "api/v2/credit/invoice-list",
      })
    );
  };

  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  async function fetchInvoiceData(page, size) {
    const res = await dispatch(
      fetchInvoices({
        url: "api/v2/credit/invoice-list",
        limit: size ?? pageSize,
        start: page ?? pageIndex,
        invoStatus: formData?.invoStatus,
        invoDate: formData?.invoDate,
        invoPayerMobileNumber: formData?.invoPayerMobileNumber,
      })
    );
  }

  useEffect(() => {
    fetchInvoiceData();
  }, []);

  return (
    <>
      <div className="w-full overflow-y-scroll h-full bg-surface">
        <InsideHeader
          title="Invoices"
          subtitle="This is a list of all invoices you have requested"
          back={false}
        />

        <div className="lg:px-10 px-3">
          <div className="flex flex-col">
            <div className="mt-[1.31rem] flex items-center gap-x-10">
              <div className="flex items-center">
                <span>
                  {" "}
                  <button
                    onClick={showModal}
                    type="button"
                    className={`bg-transparent flex items-center gap-x-'1' ${
                      Object?.keys(formData)?.length > 0
                        ? "!text-[#5688E5]"
                        : "inherit"
                    }`}
                  >
                    <MaterialIcon color="#141414" icon="filter_list" />
                    Filters
                  </button>
                </span>
                {Object?.keys(formData)?.length > 0 && (
                  <span className="flex items-center text-[#5688E5] cursor-pointer ml-1">
                    :{Object?.keys(formData)?.length}
                    <img src={svg38} alt="svg38" onClick={handleClearFilters} />
                  </span>
                )}
              </div>
            </div>
            <div className="ml-[20%]"></div>
          </div>
          {loading ? (
            <Skeleton />
          ) : (
            <div className="mt-[1.31rem] mb-10 card !p-0 overflow-hidden">
              {invoiceData && invoiceData?.length > 0 ? (
                <ResponsiveTable
                  className="w-full"
                  scroll={{
                    x: 800,
                  }}
                  pagination={{
                    position: ["bottomCenter"],
                    current: pageIndex + 1,
                    total: invoiceCount,
                    pageSize: pageSize,
                    onChange: (page, size) => {
                      setPageIndex(page - 1);
                      setPageSize(size);
                      fetchInvoiceData(page - 1, size);
                    },
                    showSizeChanger: false,
                    hideOnSinglePage: true,
                  }}
                  rowKey={(record) => record?.invoId}
                  columns={columns}
                  dataSource={invoiceData}
                  loading={loading}
                  mobileEmptyText="No invoices found"
                  mobileCard={(record) => {
                    const busy =
                      docLoadingKey &&
                      docLoadingKey.startsWith(`${record?.invoId}-`);
                    return (
                      <div className="card !p-4">
                        <div className="flex items-start justify-between gap-3">
                          <div className="min-w-0">
                            <p className="font-semibold text-primary truncate">
                              {record?.invoCode}
                            </p>
                            <p className="text-[11px] text-muted mt-1.5 truncate">
                              {dateForHumans(record?.invoCreatedDate)}
                            </p>
                          </div>
                          <div className="text-right shrink-0">
                            <p className="font-semibold whitespace-nowrap">
                              {cashConverter(record?.invoAmount)}
                            </p>
                            <div className="flex items-center justify-end mt-1.5">
                              <StatusBadge value={record?.invoStatus} />
                            </div>
                          </div>
                        </div>
                        <div className="flex justify-end mt-3">
                          <button
                            type="button"
                            className="btn-ghost px-3 py-1"
                            disabled={!!busy}
                            onClick={() => openDoc(record)}
                          >
                            {busy ? (
                              <Spin size="small" />
                            ) : (
                              <MaterialIcon
                                icon="picture_as_pdf"
                                color="#69472E"
                              />
                            )}
                          </button>
                        </div>
                      </div>
                    );
                  }}
                />
              ) : (
                <div className="flex flex-col items-center justify-center text-center py-20 px-6">
                  <div
                    className="h-20 w-20 rounded-full flex items-center justify-center"
                    style={{ background: "rgba(217,108,59,0.10)" }}
                  >
                    <MaterialIcon
                      icon="receipt_long"
                      color="var(--brand-accent)"
                      size={40}
                    />
                  </div>
                  <h3 className="mt-6 text-lg font-medium text-primary leading-snug">
                    No invoices yet
                  </h3>
                  <p className="mt-2 max-w-sm text-sm text-muted leading-relaxed">
                    {Object?.keys(formData)?.length > 0
                      ? "No invoices match the selected filters. Try a wider range or clear your filters."
                      : "You don't have any invoices yet. They'll appear here once one is created."}
                  </p>
                  {Object?.keys(formData)?.length > 0 && (
                    <button
                      onClick={handleClearFilters}
                      className="btn-outline mt-6 !py-2 !px-5"
                    >
                      Clear filters
                    </button>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      <FilterModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        formData={formData}
        setFormData={setFormData}
      />

      <InvoiceDocModal
        open={docModal.open}
        onClose={closeDoc}
        url={docModal.url}
        title={docModal.title}
        fileName={docModal.fileName}
      />
    </>
  );
}

export default Invoices;
 