import { Dropdown, Table, Tooltip } from "antd";
import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
  cashConverter,
  dateForHumans,
  formatMoney,
  numberWithCommas,
} from "../../utils";
import { useNavigate } from "react-router-dom";
import svg27 from "../../assets/svg/svg27.svg";
import {
  fetchCreditAccount,
  fetchCreditAdmin,
  fetchCreditReseller,
} from "../../features/credit/creditSlice";
import CreditAddModal from "./CreditAddModal";
import CreditAddSelfModal from "./CreditAddSelfModal";
import FilterModal from "./FilterModal";
import svg38 from "../../assets/svg/svg38.svg";
import MaterialIcon from "material-icons-react";
import svg30 from "../../assets/svg/svg30.svg";

function CreditTable() {
  const { loading, creditData, creditCount } = useSelector(
    (state) => state.credit
  );
  const { user } = useSelector((state) => state.auth);
  const { resellerId } = useSelector((state) => state.global);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [prodd, setProdd] = useState("");

  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };

  const [isModalOpenSelf, setIsModalOpenSelf] = useState(false);
  const showModalSelf = () => {
    setIsModalOpenSelf(true);
  };
  //
  const [isModalOpenFilter, setIsModalOpenFilter] = useState(false);
  const showModalFilter = () => {
    setIsModalOpenFilter(true);
  };
  const [formData, setFormData] = useState({});

  const hasResellerName = creditData?.some(
    (item) =>
      item?.smsResellerName !== null && item?.smsResellerName !== undefined
  );

  const hasAccountName = creditData?.some(
    (item) =>
      item?.smsAccountName !== null && item?.smsAccountName !== undefined
  );

  const columns = [
    {
      title: "Created Date",
      render: (item) => {
        return <div>{dateForHumans(item)}</div>;
      },
      dataIndex: "smsCreatedDate",
    },
    ...(hasResellerName
      ? [
          {
            title: "Reseller Name",
            render: (item) => {
              return <div>{item}</div>;
            },
            dataIndex: "smsResellerName",
          },
        ]
      : []),
    ...(hasAccountName
      ? [
          {
            title: "Account Name",
            render: (item) => {
              return <div>{item}</div>;
            },
            dataIndex: "smsAccountName",
          },
        ]
      : []),

    {
      title: "Previous Balance",
      render: (item) => {
        return <div>{cashConverter(item)}</div>;
      },
      dataIndex: "smsPrevBal",
    },
    {
      title: "Sms Rate",
      dataIndex: "smsRate",
    },

    {
      title: "Amount",
      render: (item) => {
        return <div>{cashConverter(item)}</div>;
      },
      dataIndex: "smsPayAmount",
    },

    {
      title: "Loaded Unit",
      render: (item) => {
        return <div>{numberWithCommas(item)}</div>;
      },
      dataIndex: "smsLoaded",
    },
    {
      title: "New Balance",
      render: (item) => {
        return <div>{cashConverter(item)}</div>;
      },
      dataIndex: "smsNewBal",
    },
    {
      title: "Created By",
      dataIndex: "smsCreatedByName",
    },
    {
      title: "Status",
      render: (item) => {
        return <div className="font-bold">{item}</div>;
      },
      dataIndex: "crStatus",
    },
  ];

  const [isModalOpenDelete, setIsModalOpenDelete] = useState(false);
  const showModalDelete = async () => {
    setIsModalOpenDelete(true);
  };

  const [isModalOpenEdit, setIsModalOpenEdit] = useState(false);
  const showModalEdit = async () => {
    setIsModalOpenEdit(true);
  };

  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  function handleRequestView() {
    navigate(`/product-request-view/${prodd?.reId}`);
  }

  const settingItems = [
    {
      key: "0",
      label: (
        <div className=" mb-1 flex text-[16px] font-sans items-center justify-center  text-darkGreen">
          View
        </div>
      ),
    },
    {
      key: "1",
      label: (
        <div className=" flex  text-[16px] font-sans items-center justify-center text-darkGreen">
          Edit
        </div>
      ),
    },
  ];

  const handleClick = async (item) => {
    // const res = await dispatch(
    //   downloadExcel({
    //     url: "api/v2/sms/download-excel",
    //     msgStatus: formData?.msgStatus,
    //     msgCreatedDate: formData?.msgCreatedDate,
    //     msgSubmobileNo: formData?.msgSubmobileNo,
    //     msgMessage: formData?.msgMessage,
    //     msgAccId: formData?.msgAccId,
    //     msgSenderId: formData?.msgSenderId,
    //   })
    // );

    // if (res?.payload) {
    //   const blob = new Blob([res.payload], {
    //     type: "application/octet-stream",
    //   });
    //   const url = window.URL.createObjectURL(blob);
    //   const link = document.createElement("a");
    //   link.href = url;
    //   link.setAttribute("download", "sent-sms-list.xlsx");
    //   document.body.appendChild(link);
    //   link.click();
    //   link.parentNode.removeChild(link);
    //   toast.success("File downloaded successfully");
    // } else {
    //   toast.error("Failed to download file");
    // }
  };

  const handleClearFilters = async () => {
    await setFormData({});
    const res = await dispatch(
      fetchCreditReseller({
        url:user?.layer === "TOP" ? "api/v2/credit/reseller" : user?.layer === "RESELLER" ? "api/v2/credit/reseller-account": user?.layer === "ACCOUNT"? "api/v2/credit/account" : null,
        // smsResellerName: null,
        smsAccountName: null,
        smsCreatedByName: null,
        smsCreatedDate: null,
        smsApprovedByName: null,
        smsApprovedDate: null,
        crStatus: null,
      })
    );
  };

  async function fetchCreditData(page, size) { 
      const res = await dispatch(
        fetchCreditReseller({
          url:user?.layer === "TOP" ? "api/v2/credit/reseller" : user?.layer === "RESELLER" ? "api/v2/credit/reseller-account": user?.layer === "ACCOUNT"? "api/v2/credit/account" : null,
          limit: size ?? pageSize,
          start: page ?? pageIndex,
          // smsResellerName: formData?.smsResellerName,
          smsAccountName: formData?.smsAccountName,
          smsCreatedByName: formData?.smsCreatedByName,
          smsCreatedDate: formData?.smsCreatedDate,
          smsApprovedByName: formData?.smsApprovedByName,
          smsApprovedDate: formData?.smsApprovedDate,
          crStatus: formData?.crStatus,
        })
      ); 
  }


  useEffect(() => {
    fetchCreditData();
  }, []);

  return (
    <>
    <div className="flex justify-between ">
      <div className=" flex items-center gap-x-5">
        <div className="w-[250px]">
          <button
            className="cstm-btn !rounded-[4px] !bg-[#A3A2A7] !text-[.75rem] flex items-center gap-x-3"
            onClick={showModal}
          >
            <img src={svg30} alt="svg30" />
            Load Account Credit
          </button>
        </div>
        {user?.layer === "RESELLER" && (
          <div className="w-[250px]">
            <button
              className="cstm-btn !rounded-[4px] !bg-[#A3A2A7] !text-[.75rem] flex items-center gap-x-3"
              onClick={showModalSelf}
            >
              <img src={svg30} alt="svg30" />
              Load Self Credit
            </button>
          </div>
        )}
        <div className="flex items-center">
          <span>
            {" "}
            <button
              onClick={showModalFilter}
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
      <div className="flex justify-end">
      {creditData?.length > 0 && (
                <div className="flex justify-end item-center"> 
                  <Tooltip placement="top" title={"Download Excel"}>  
                    <button onClick={handleClick} className="flex items-center"> 
                      <MaterialIcon size={45} color="#00B050" icon="article" />
                      <span>Export to excel</span>
                    </button>
                  </Tooltip>
                </div>
              )}
      </div>
      </div>
      <Table
        className="mt-[1.31rem] w-full"
        scroll={{
          x: 800,
        }}
        rowKey={(record) => record?.reId}
        columns={columns}
        dataSource={creditData}
        loading={loading}
        pagination={{
          position: ["bottomCenter"],
          current: pageIndex + 1,
          total: creditCount,
          pageSize: pageSize,
          onChange: (page, size) => {
            setPageIndex(page - 1);
            setPageSize(size);
            fetchCreditData(page - 1, size);
          },
          showSizeChanger: false,
          hideOnSinglePage: true,
        }}
      />
      <CreditAddModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        prodd={prodd}
      />

      <CreditAddSelfModal
        isModalOpen={isModalOpenSelf}
        setIsModalOpen={setIsModalOpenSelf}
        prodd={prodd}
      />

      <FilterModal
        isModalOpen={isModalOpenFilter}
        setIsModalOpen={setIsModalOpenFilter}
        formData={formData}
        setFormData={setFormData}
      />
    </>
  );
}

export default CreditTable;
