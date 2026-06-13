import React, { useEffect, useState } from "react";
import ResponsiveTable, { hideBelow } from "../../components/ResponsiveTable";
import { fetchPayoutHistory } from "../../features/billing/billingSlice";
import FilterModal from "./FilterModal";
import MaterialIcon from "material-icons-react";
import { useDispatch, useSelector } from "react-redux";
import svg38 from "../../assets/svg/svg38.svg";
import { dateForHumans, getDate90DaysAgo } from "../../utils";

 
function PayoutHistory() {
  const { payoutHistoryCount, payoutHistoryData } = useSelector(
    (state) => state.billing
  );
  const dispatch =  useDispatch()
  const [formData, setFormData] = useState({});
  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };

  const handleClearFilters = async (page, size) => {
    await setFormData({});
    await dispatch(
      fetchPayoutHistory({
        url: "api/v2/wallet/withdrawals",
        withDrawDateFrom: getDate90DaysAgo(),
      })
    );
  };

  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  async function fetchPayoutHistoryData(page, size) {
    const res = await dispatch(
      fetchPayoutHistory({
        url: "api/v2/wallet/withdrawals",
        limit: size ?? pageSize,
        start: page ?? pageIndex,
        withDrawStatus: formData?.withDrawStatus,
        withDrawDateFrom: getDate90DaysAgo(),
        withDrawDateTo: formData?.withDrawDateTo,
        withDrawSubmobileNo: formData?.withDrawSubmobileNo,
      })
    );
  }

  useEffect(() => {
    fetchPayoutHistoryData();
  }, []);

  const columns = [
    {
      title: "Amount",
      dataIndex: "withDrawAmount", 
    },
    {
      title: "Payout Date",
      render:(item)=>{
        return(
          <div>{dateForHumans(item)}</div>
        )
      },
      dataIndex: "withDrawCreatedDate", 
    }, 
    {
      title: "Status",
      dataIndex: "withDrawStatus",
      render: (item) => {
        return (
          <div
            className={`${item === 'FAILED' ? 'bg-[#FFA500]' : 'bg-primary'}  text-[#FFf]  inline-block rounded-[5px] px-2 py-1 w-auto`}
          >
            {item}
          </div>
        );
      }, 
    },
    {
      title: "Phone Number",
      dataIndex: "withDrawPhoneNumber",
      ...hideBelow(),
    },
    {
      title: "Withdrawal Email",
      dataIndex: "withDrawCreatedByEmail",
      ...hideBelow(),
    },
  ];

 
  return (
    <div> 
      <div className="product_sub !text-[18px] mt-3">Payout History</div>
      <div className="mt-[.31rem] flex items-center gap-x-10">
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
      <ResponsiveTable
        dataSource={payoutHistoryData}
        columns={columns}
        className="mt-[.31rem]  mb-[10rem] w-full"
        scroll={{
          x: 800,
        }}
        pagination={{
          position: ["bottomCenter"],
          current: pageIndex + 1,
          total: payoutHistoryCount,
          pageSize: pageSize,
          onChange: (page, size) => {
            setPageIndex(page - 1);
            setPageSize(size);
            fetchPayoutHistoryData(page - 1, size);
          },
          showSizeChanger: false,
          hideOnSinglePage: true,
        }}
        rowKey={(record) => record?.withDrawId}
      />
      <FilterModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        formData={formData}
        setFormData={setFormData}
      />
    </div>
  );
}

export default PayoutHistory;
