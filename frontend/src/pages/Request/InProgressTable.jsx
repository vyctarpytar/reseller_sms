import { Dropdown } from "antd";
import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import ResponsiveTable, { hideBelow } from "../../components/ResponsiveTable";
import { dateForHumans } from "../../utils";
import { fetchInProgressProductRequest, fetchNewProductRequest } from "../../features/product-request/productRequestSlice";
import { useNavigate } from "react-router-dom";
import DeleteModal from "../../components/DeleteModal";
import svg27 from '../../assets/svg/svg27.svg'

function InProgressTable() {
  const { refetchKey, progressData } = useSelector((state) => state.productRequest);
  const dispatch = useDispatch();
  const navigate = useNavigate();
 
  const [prodd, setProdd] = useState("");

  const columns = [
    {
      title: "Reference No",
      dataIndex: "reResellerId",
      ...hideBelow(),
    },
    {
      title: "Reseller Name",
      dataIndex: "reName",
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
    navigate(`/product-request-view/${prodd?.reId}`)
  }

  const settingItems = [
    {
      key: "0",
      label: (
        <div className=" mb-1 flex items-center text-[16px] font-sans justify-center  text-primary"
        onClick={handleRequestView}>
          View
        </div>
      ),
       
    },
    {
      key: "1",
      label: (
        <div
          className=" flex items-center text-[16px] font-sans justify-center text-primary" 
        >
         Edit
        </div>
      ),
      
    },
  ];

  const handleDelete=()=>{
    console.log('delete')
  }

 
  return (
    <>
      <ResponsiveTable
        className="mt-[1.31rem] w-full"
        scroll={{
          x: 800,
        }}
        rowKey={(record) => record?.reId}
        columns={columns}
        dataSource={progressData}
        mobileEmptyText="No requests found"
        mobileCard={(record) => (
          <div className="card !p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="font-semibold truncate">{record?.reName}</p>
                <p className="text-[11px] text-muted mt-1.5 truncate">
                  {dateForHumans(record?.reCreatedDate)}
                  {record?.reResellerId ? ` · ${record.reResellerId}` : ""}
                </p>
              </div>
              <div className="text-right shrink-0 flex items-start gap-2">
                <span className="text-[12px] font-medium whitespace-nowrap">
                  {record?.reStatus}
                </span>
                <button onClick={() => setProdd(record)}>
                  <Dropdown
                    overlayStyle={{
                      width: "150px",
                    }}
                    trigger={"click"}
                    menu={{ items: settingItems }}
                    placement="bottomRight"
                  >
                    <img src={svg27} alt="svg27" />
                  </Dropdown>
                </button>
              </div>
            </div>
          </div>
        )}
      />

      <DeleteModal
        isModalOpen={isModalOpenDelete}
        setIsModalOpen={setIsModalOpenDelete}
        prodd={prodd}
        handleDelete={handleDelete}
        loading="loading"
        content={`Are you sure you want to delete activity {prodd?.jaaTitle}?`}
      />
    </>
  );
}

export default InProgressTable;
