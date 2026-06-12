import { Badge, Dropdown, Skeleton, Table, Tooltip } from "antd";
import React, { useEffect, useRef, useState } from "react";
import InsideHeader from "../../components/InsideHeader";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import MaterialIcon from "material-icons-react";
import { addSpaces, cashConverter, dateForHumans } from "../../utils";
import noCon from "../../assets/img/noCon.png";
import svg38 from "../../assets/svg/svg38.svg";
import FilterModal from "./FilterModal";
import { fetchInvoices } from "../../features/invoice/invoiceSlice";
import AccpetanceModal from "./AccpetanceModal";

function Invoices() {
  const [notOpen, setnotOpen] = useState(false);
  const { sentSmsData, sentSmsCount } = useSelector((state) => state.save);
  const { invoiceData, invoiceCount, loading } = useSelector(
    (state) => state.inv
  );
  const { user } = useSelector((state) => state.auth);
 
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const [formData, setFormData] = useState({});
  const handleOpenChange = () => {
    setnotOpen(false);
  };

  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };

  const [isModalAcceptance, setIsModalAcceptance] = useState(false);
  const showModalConfirmation = async () => {
    setIsModalAcceptance(true);
  };

  const hasApprove = user?.role ==="ACCOUNTANT" || user?.role ==="SUPER_ADMIN" 

  const [prodd,setProdd] =  useState()

  const columns = [
    {
      title: "Code",
      dataIndex: "invoCode",
    },
    {
      title: "Amount",
      dataIndex: "invoAmount",
    },
    // {
    //   title: "Tax Rate",
    //   dataIndex: "invoTaxRate",
    // },
    // {
    //   title: "Amount After Tax",
    //   dataIndex: "invoAmountAfterTax",
    // },
    {
      title: "Pay Mobile",
      render: (item) => {
        return <div>{addSpaces(item)}</div>;
      },
      dataIndex: "invoPayerMobileNumber",
    },
    {
      title: "Created By Email",
      dataIndex: "invoCreatedByEmail",
    },
    {
      title: "Created Date",
      dataIndex: "invoCreatedDate",
    },
    {
      title: "Due Date",
      dataIndex: "invoDueDate",
    },
    {
      title: "Status",
      render: (item) => {
        return (
          <div
            className={`${
              item?.invoStatus == "PAID"
                ? "text-[#388E3C]"
                : item?.invoStatus == "PENDING_PAYMENT"
                ? "text-[#ff0000]"
                : "text-blk14"
            }  
         font-[700] flex items-center justify-center text-center`}
          >
            {item?.invoStatus}
          </div>
        );
      },
    },

    ...(hasApprove
      ? [
        {
          title: "Actions",
          render: (item) => (
            <>
              <button onClick={() => setProdd(item)}>
                <div
                  className="text-darkGreen h-auto py-1 px-3 rounded-[20px] w-full border border-[#388E3C]"
                  onClick={showModalConfirmation}
                >
                  {
                  item?.invoStatus != "PAID" && "Approve"}
                  
                </div>
              </button>
            </>
          ),
        },
      ]
      : []), 
   
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
      <div className="w-full overflow-y-scroll h-full">
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
            <div>
              {sentSmsData && sentSmsData?.length > 0 ? (
                <Table
                  className="mt-[1.31rem] w-full mb-10"
                  scroll={{
                    x: 800,
                  }}
                  pagination={{
                    position: ["bottomCenter"],
                    current: pageIndex + 1,
                    total: sentSmsCount,
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
                  dataSource={sentSmsData}
                  loading={loading}
                />
              ) : (
                <div className="flex flex-col items-center justify-center mt-10">
                  <img src={noCon} alt="noCon" />
                  <span className="text-black21 text-[18px] font-normal leading-[24px] font-dmSans">
                    No Sms Found
                  </span>
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

      <AccpetanceModal
        isModalOpen={isModalAcceptance}
        setIsModalOpen={setIsModalAcceptance}
        prodd={prodd}
        title={`Approve ${prodd?.invoCode}`} 
      />
    </>
  );
}

export default Invoices;
 