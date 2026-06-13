import { Dropdown, Tag } from "antd";
import React, { useEffect, useState } from "react";
import ResponsiveTable, { hideBelow } from "../../components/ResponsiveTable";
import { useDispatch, useSelector } from "react-redux";
import { dateForHumans } from "../../utils";
import { fetchNewProductRequest } from "../../features/product-request/productRequestSlice";
import { useNavigate } from "react-router-dom";
import DeleteModal from "../../components/DeleteModal";
import svg27 from '../../assets/svg/svg27.svg'
import { save } from "../../features/save/saveSlice";
import toast from "react-hot-toast";

function NewTable() {
  const {  newData } = useSelector((state) => state.productRequest);
  const { refetchKey } = useSelector((state) => state.resellerCodes);
  const { saving } = useSelector((state) => state.save);
  const dispatch = useDispatch();
  const navigate = useNavigate();
 
  const [prodd, setProdd] = useState("");

  const columns = [
    {
      title: "Reference No",
      dataIndex: "reResellerId", 
    },
    {
      title: "Telcos",
      dataIndex: "reTelcos",
      ...hideBelow(),
    },
    {
      title: "Type",
      dataIndex: "reServiceType",
      ...hideBelow(),
    },
    {
      title: "Status",
      dataIndex: "reStatus", 
    },
    {
      title: "Created Date",
      render: (item) => {
        return <div>{dateForHumans(item)}</div>;
      },
      dataIndex: "reCreatedDate", 
    },
   
    {
      title: "Actions",
      render: (item) => (
        <>
          <button onClick={()=>setProdd(item)}>
            <Dropdown
              overlayStyle={{
                width: "150px",
              }}
              trigger={"click"}
              menu={{ items: settingItems }}
              placement="bottom"
            >
              <img src={svg27} alt="svg27"/> 
            </Dropdown>
          </button>
        </>
      ),
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

  function handleRequestView(){
    navigate(`/reseller-code-view/${prodd?.reId}`)
  }

  const settingItems = [
    {
      key: "0",
      label: (
        <div className=" mb-1 flex text-[16px] font-sans items-center justify-center  text-primary"
        onClick={handleRequestView}>
          View
        </div>
      ),
       
    },
    {
      key: "1",
      label: (
        <div
          className=" flex  text-[16px] font-sans items-center justify-center text-primary" 
          onClick={showModalDelete}
        >
         Delete
        </div>
      ),
      
    },
  ];

  const handleDelete=async()=>{
    const res = await dispatch(
      save({
        url: `api/v2/req/invalidate/${prodd?.reId}`, 
      })
    ); 
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message); 
      fetchNewProductRequestData(); 
      await setIsModalOpenDelete(true);
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  }

  async function fetchNewProductRequestData() {
    const res = await dispatch(
      fetchNewProductRequest({
        reqStatus: "PENDING",
      })
    );
  }
 
  return (
    <>
      <ResponsiveTable
        className="mt-[1.31rem] w-full"
        scroll={{
          x: 800,
        }}
        // pagination={{
        //   position: ["bottomCenter"],
        //   current: pageIndex + 1,
        //   total: newCount,
        //   pageSize: pageSize,
        //   onChange: (page, size) => {
        //     setPageIndex(page - 1);
        //     setPageSize(size);
        //     fetchProductRequestData(page - 1, size);
        //   },
        //   showSizeChanger: false,
        //   hideOnSinglePage: true,
        // }}
        rowKey={(record) => record?.reId}
        columns={columns}
        dataSource={newData}
        mobileEmptyText="No reseller codes found"
        mobileCard={(record) => {
          const status = record?.reStatus;
          const isApproved = `${status}`.toUpperCase() === "APPROVED";
          const isRejected = `${status}`.toUpperCase() === "REJECTED";
          const pillClass = isApproved
            ? "!bg-[#EAF6EC] !text-[#2A662C]"
            : isRejected
            ? "!bg-[#FDECEC] !text-[#C0392B]"
            : "!bg-[#FFF6E5] !text-[#B8860B]";
          return (
            <div className="card !p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="font-semibold truncate">{record?.reResellerId}</p>
                  <p className="text-[11px] text-[#777] mt-1.5 truncate">
                    {dateForHumans(record?.reCreatedDate)}
                    {record?.reTelcos ? ` · ${record.reTelcos}` : ""}
                    {record?.reServiceType ? ` · ${record.reServiceType}` : ""}
                  </p>
                </div>
                <div className="text-right shrink-0 flex items-center gap-2">
                  <Tag className={`!border-0 !rounded-[6px] !text-[11px] whitespace-nowrap ${pillClass}`}>
                    {status}
                  </Tag>
                  <button onClick={() => setProdd(record)}>
                    <Dropdown
                      overlayStyle={{
                        width: "150px",
                      }}
                      trigger={"click"}
                      menu={{ items: settingItems }}
                      placement="bottom"
                    >
                      <img src={svg27} alt="svg27" />
                    </Dropdown>
                  </button>
                </div>
              </div>
            </div>
          );
        }}
      />

      <DeleteModal
        isModalOpen={isModalOpenDelete}
        setIsModalOpen={setIsModalOpenDelete}
        prodd={prodd}
        handleDelete={handleDelete}
        loading={saving}
        content={`Are you sure you want to delete ${prodd?.reId}?`}
      />
    </>
  );
}

export default NewTable;
