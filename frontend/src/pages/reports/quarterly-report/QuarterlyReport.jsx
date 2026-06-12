import {
  Checkbox,
  DatePicker,
  Divider,
  Dropdown,
  Form,
  Select,
  Skeleton,
  Spin,
  Table,
  Tag,
  Tooltip,
} from "antd";
import React, { useEffect, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { numberWithCommas } from "../../../utils";
import MaterialIcon from "material-icons-react";
import { downloadExcel } from "../../../features/save/saveSlice";
import toast from "react-hot-toast";
import TableLoading from "../../../components/TableLoading";
import { fetchData } from "../../../features/global/globalSlice";
import svg38 from "../../../assets/svg/svg38.svg";
import FilterModal from "./FilterModal";
import Cards from "./Cards";
import moment from "moment";
import { fetchReseller } from "../../../features/reseller/resellerSlice";
import { fetchResellerAccounts } from "../../../features/reseller-account/resellerAccountSlice";

function QuarterlyReport() {
  const [selectId, setSelectId] = useState(new Date().getFullYear());
  const [formData, setFormData] = useState({});
  const dispatch = useDispatch();
  const [form] = Form.useForm();
  const formRef = useRef(null);
  const urlYear = "api/v2/annual-reports/years";
  const urlCard = `api/v2/annual-reports/summary?year=${selectId}&quarter=${
    formData?.quarter ?? null
  }&accountId=${formData?.accountId ?? null}&resellerId=${
    formData?.resellerId ?? null
  }`;
  const urlGrid = `api/v2/annual-reports?year=${selectId}&quarter=${
    formData?.quarter ?? null
  }&accountId=${formData?.accountId ?? null}&resellerId=${
    formData?.resellerId ?? null
  }`;

  const { dataYear, dataCard, dataGrid, countGrid, loading } = useSelector(
    (state) => ({
      dataYear: state?.global?.data?.[urlYear] || [],
      dataCard: state?.global?.data?.[urlCard] || [],
      dataGrid: state?.global?.data?.[urlGrid] || [],
      countGrid: state?.global?.count?.[urlGrid] || 0,
      loading: state?.global?.loading,
    })
  );
  

  const { user } = useSelector((state) => state.auth);
  const { saving } = useSelector((state) => state.save);

  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [initialLoad, setInitialLoad] = useState(true);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };

  const today = new Date();

  const plainOptions = [
    { value: "PUBLISH", label: "Published" },
    { value: "RE_ADVERTISE", label: "Re-advertised" },
    { value: "ARCHIEVE", label: "Archived" },
    { value: "CANCEL", label: "Cancelled" },
  ];

  const [selectedFilters, setSelectedFilters] = useState([]);
  const [popoverVisible, setPopoverVisible] = useState(false);

  const handleRemoveFilter = (removedStatus) => (e) => {
    console.log("clicked");
    e.preventDefault();
    setSelectedFilters((prev) =>
      prev?.filter((status) => status !== removedStatus)
    );
  };
  const onChangeFilter = (checkedValues) => {
    setSelectedFilters(checkedValues);
  };

  const handleApply = async () => {
    await setPopoverVisible(false);
  };

  const handleReset = async () => {
    await setSelectedFilters([]);
    await setPopoverVisible(false);
  };
  const filterOverlay = (
    <div className="w-[15.6875rem] p-[1.5rem] bg-white rounded-[.5rem] shadow-[0_0_13.9px_0_rgba(0,0,0,0.08)]">
      <p className="font-semibold">Select Filter</p>
      <Checkbox.Group
        options={plainOptions}
        value={selectedFilters}
        onChange={onChangeFilter}
        className="filter-checkbox"
      />

      <Divider />

      <div className="flex justify-center gap-[1.5rem] mt-3 font-dmSans text-[.875rem] font-[400]">
        <span className="text-[#FDB505]  cursor-pointer" onClick={handleReset}>
          Reset
        </span>
        <span className="text-[#C62130]  cursor-pointer" onClick={handleApply}>
          Apply
        </span>
      </div>
    </div>
  );

  const columns = [
    { title: "Reseller", dataIndex: "resellerName" },
    { title: "Account", dataIndex: "accountName" },
    { title: "Sender ID", dataIndex: "senderId" },
    { title: "Provider", dataIndex: "senderIdProvider" },
    { title: "Period", dataIndex: "period" },
    { title: "Messages", dataIndex: "messages" },
    { title: "Delivered", dataIndex: "delivered" },
    { title: "Failed", dataIndex: "failed" },
    { title: "Delivery Rate", dataIndex: "deliveryRate" },
    { title: "Revenue", dataIndex: "revenue" },
    { title: "Avg Msg Cost", dataIndex: "averageMessageCost" },
    { title: "Unit Price", dataIndex: "unitPrice" }, 
    { title: "Unique Customers", dataIndex: "uniqueCustomerCount" },
    { title: "Top Month", dataIndex: "topPerformingMonth" },
    { title: "Status", dataIndex: "status" },
    {
      title: "Updated",
      dataIndex: "updatedAt",
      render: (text) =>
        text ? moment(text)?.format("DD-MM-YYYY HH:mm:ss") : "-",
    },
  ];
  const [expandedRowKeys, setExpandedRowKeys] = useState([]);

  const tableData = dataGrid?.map((item) => ({
    key: item?.id,
    resellerName: item?.resellerName,
    accountName: item?.accountName,
    senderId: item?.senderId,
    senderIdProvider: item?.senderIdProvider,
    period: `Q${item.quarter} ${item.year}`,
    messages: item?.quarterTotalMessages,
    delivered: item?.quarterDeliveredCount,
    failed: item?.quarterFailedCount,
    deliveryRate: `${item.quarterDeliveryRate}%`,
    revenue: item?.quarterTotalRevenue,
    averageMessageCost: item?.averageMessageCost,
    unitPrice: item?.unitPrice,
    uniqueCustomerCount: item?.uniqueCustomerCount,
    topPerformingMonth: item?.topPerformingMonth,
    status: item?.status,
    updatedAt: item?.updatedAt,
    children: [
      {
        key: `${item.id}-1`,
        period: item?.month1?.monthName,
        messages: item?.month1?.messageCount,
        delivered: item?.month1?.deliveredCount,
        failed: item?.month1?.failedCount,
        deliveryRate: `${item.month1?.deliveryRate}%`,
        revenue: item?.month1?.revenue,
      },
      {
        key: `${item.id}-2`,
        period: item?.month2?.monthName,
        messages: item?.month2?.messageCount,
        delivered: item?.month2?.deliveredCount,
        failed: item?.month2?.failedCount,
        deliveryRate: `${item.month2?.deliveryRate}%`,
        revenue: item?.month2?.revenue,
      },
      {
        key: `${item.id}-3`,
        period: item?.month3?.monthName,
        messages: item?.month3?.messageCount,
        delivered: item?.month3?.deliveredCount,
        failed: item?.month3?.failedCount,
        deliveryRate: `${item.month3?.deliveryRate}%`,
        revenue: item?.month3?.revenue,
      },
    ],
  }));
  const handleValuesChange = async (value) => {
    await setSelectId(value);
  };

  const handleClick = async (item) => {
    const res = await dispatch(
      downloadExcel({
        url: `api/v2/annual-reports/export/excel?year=${selectId}&quarter=${
          formData?.quarter ?? null
        }&accountId=${formData?.accountId ?? null}&resellerId=${
          formData?.resellerId ?? null
        }`,
      })
    );

    if (res?.payload) {
      const blob = new Blob([res.payload], {
        type: "application/octet-stream",
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", "quartely-report.xlsx");
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      toast.success("File downloaded successfully");
    } else {
      toast.error("Failed to download file");
    }
  };
  async function fetchTableData(page, size) {
    dispatch(
      fetchData({
        url: urlGrid,
        limit: size ?? pageSize,
        start: page ?? pageIndex,
      })
    );
  }

  const handleClearFilters = async () => {
    await setFormData({});
  };

  useEffect(() => {
    fetchTableData();
    dispatch(
      fetchData({
        url: urlCard,
      })
    );
  }, [selectId, formData]);

  useEffect(() => {
    dispatch(
      fetchData({
        url: urlYear,
      })
    );
    dispatch(fetchResellerAccounts());
    dispatch(fetchReseller());
  }, []);

  if (loading && !isModalOpen) return <TableLoading />;
  return (
    <div className="w-full h-full overflow-y-scroll lg:px-10 px-3">
      <div className="flex lg:flex-row flex-col-reverse lg:items-center  gap-2 mt-10  justify-between">
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
              Advanced Filters
            </button>
          </span>
          {Object?.keys(formData)?.length > 0 && (
            <span className="flex items-center text-[#5688E5] cursor-pointer ml-1">
              :{Object?.keys(formData)?.length}
              <img src={svg38} alt="svg38" onClick={handleClearFilters} />
            </span>
          )}
        </div>
        <div className="w-full lg:w-[20%]">
          {" "}
          <Form
            layout="vertical"
            ref={formRef}
            name="control-ref"
            className=" w-full"
            form={form}
          >
            <div className="w-full banner-select flex items-center gap-x-[.5rem] text-[#6B7280] text-[.875rem] font-[400] font-interSans">
              <span className="">Year</span>
              <Select
                className="banner-selector"
                allowClear
                style={{
                  width: "100%",
                }}
                options={dataYear?.map((item) => ({
                  value: item,
                  label: item,
                }))}
                value={selectId}
                onChange={(value) => handleValuesChange(value)}
                placeholder="Select"
                showSearch
                optionFilterProp="children"
                filterOption={(input, option) =>
                  (option?.label?.toLocaleLowerCase() ?? "").includes(
                    input?.toLocaleLowerCase()
                  )
                }
              />
            </div>
          </Form>
        </div>
      </div>

      <Cards dataCard={dataCard} />
      <div className="flex justify-end item-center w-full mt-[1rem]">
        <Tooltip placement="top" title={"Download Excel"}>
          {saving ? (
            <Spin className="sms-spin" />
          ) : (
            <button
              disabled={saving}
              onClick={handleClick}
              className="flex items-center"
            >
              <MaterialIcon size={45} color="#00B050" icon="article" />
              <span>Export to excel</span>
            </button>
          )}
        </Tooltip>
      </div>
      <Table
        className="mt-[1.31rem] w-full"
        scroll={{ x: "2000px" }}
        rowKey={(record) => record?.key}
        columns={columns}
        dataSource={tableData}
        expandable={{
          expandedRowKeys,
          onExpand: (expanded, record) => {
            if (expanded) {
              setExpandedRowKeys([record.key]);
            } else {
              setExpandedRowKeys([]);
            }
          },
        }}
        loading={loading}
        pagination={{
          position: ["bottomCenter"],
          current: pageIndex + 1,
          total: countGrid,
          pageSize: pageSize,
          onChange: (page, size) => {
            setPageIndex(page - 1);
            setPageSize(size);
            fetchTableData(page - 1, size);
          },
          showSizeChanger: false,
          hideOnSinglePage: true,
        }}
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

export default QuarterlyReport;
