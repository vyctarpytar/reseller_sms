import React, { useEffect, useState } from "react";
import InsideHeader from "../../components/InsideHeader";
import {  Table, Tooltip } from "antd";
import { useDispatch, useSelector } from "react-redux"; 
import MaterialIcon from "material-icons-react";
import svg38 from "../../assets/svg/svg38.svg";
import svg27 from "../../assets/svg/svg27.svg";
import FilterModal from "./FilterModal";
import SenderIdModal from "./SenderIdModal";
import { fetchResellerAccounts } from "../../features/reseller-account/resellerAccountSlice";
import { cashConverter, dateForHumans, numberWithCommas } from "../../utils";
import AddSenderIdModal from "./AddSenderIdModal";

function SenderList() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };

  const [isModalOpenAdd, setIsModalOpenAdd] = useState(false);
  const showModalAdd = () => {
    setIsModalOpenAdd(true);
  };


  const [isModalOpenPermission, setIsModalOpenPermission] = useState(false);
  const showModalPermission = () => {
    setIsModalOpenPermission(true);
  };

  const [prodd, setProdd] = useState();
  const { resellerAccountData,loading } = useSelector((state) => state.resellerAccount);


  const dispatch = useDispatch();
 
  const colors = ["#FF5733", "#33FF57", "#3357FF", "#FF33A5", "#A533FF"];

  const columns = [
    {
      title: "Account Name",
      dataIndex: "accName", 
    },
    {
      title: "Email Address",
      dataIndex: "accAdminEmail", 
    },
    {
      title: "Admin Mobile",
      dataIndex: "accAdminMobile", 
    },
    {
      title: "Office Mobile",
      dataIndex: "accOfficeMobile", 
    },
    
    {
      title: "Sms Price",
      render:(item)=>{
        return(
          <div>{cashConverter(item)}</div>
        )
      },
      dataIndex: "accSmsPrice", 
    }, 
    {
      title: "Balance",
      render:(item)=>{
        return(
          <div>{numberWithCommas(item)}</div>
        )
      },
      dataIndex: "accMsgBal", 
    }, 
    
    
    {
      title: "Created Date",
      render: (item) => {
        return <div>{dateForHumans(item)}</div>;
      },
      dataIndex: "accCreatedDate", 
    },
    {
      title: "Assigned",
      render: (item) => {
        return (
          <div>
            {item?.senderId?.map((acc, index) => {
                const randomColor = colors[Math.floor(Math.random() * colors.length)];
                return(
                  <div key={acc?.shId}>
                  <div className="w-auto rounded-[30px] 
                   bg-lightBlue flex flex-col mb-[.5rem] py-1 px-3 items-start justify-start text-[14px]"
                   style={{color:randomColor}}>{acc?.shCode}</div>
                  </div>
                )
            }
             
            )}
          </div>
        );
      },
    },
   
    {
      title: "Actions",
      render: (item) => (
        <>
          <button onClick={()=>setProdd(item)}>
            <div className="text-darkGreen" onClick={showModalPermission}>
              <Tooltip title="Assign Sender ID">
              <img src={svg27} alt="svg27"/>
              </Tooltip>
            </div> 
          </button>
        </>
      ),
    },
  ];

   
  const [formData, setFormData] = useState({});
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  function fetchResellerAccountData() {
    dispatch(fetchResellerAccounts());
  }

  // const handleClearFilters = async (page, size) => {
  //   await setFormData({});
  //   await dispatch(
  //     fetchSenderIds({
  //       url: "api/v2/setup",
  //     })
  //   );
  // };

  const handleClearFilters = async (page, size) => { 
    await setFormData({}); 
   await fetchResellerAccountData()
  };

  useEffect(() => {
    fetchResellerAccountData();
  }, []);

 

  return (
    <div className="w-full h-full overflow-y-scroll">
      <InsideHeader
        title="List of Sender ID"
        subtitle="Map Sender ID to Account"
        back={true}
      />

      <div className="lg:px-10 px-3">
        <div className="flex items-center justify-between  mt-10 mb-2"> 
        <div className="flex justify-start items-center">
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
            <div className="w-[200px]">
            <button
              className="cstm-btn   !bg-darkGreen   flex items-center gap-x-3"
              onClick={showModalAdd}
            >
              Add Sender ID
            </button>
          </div> 
        </div>
        <Table
          className="mt-[1px] w-full"
          scroll={{
            x: "max-content",
          }}
          // pagination={{
          //   position: ["bottomCenter"],
          //   current: pageIndex + 1,
          //   total: senderIdCount,
          //   pageSize: pageSize,
          //   onChange: (page, size) => {
          //     setPageIndex(page - 1);
          //     setPageSize(size);
          //     fetchSenderData(page - 1, size);
          //   },
          //   showSizeChanger: false,
          //   hideOnSinglePage: true,
          // }}
          rowKey={(record) => record?.accId}
          columns={columns}
          dataSource={resellerAccountData}
          loading={loading}
        />
      </div>

      <FilterModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        formData={formData}
        setFormData={setFormData}
      />

      <SenderIdModal
        isModalOpen={isModalOpenPermission}
        setIsModalOpen={setIsModalOpenPermission}
        prodd={prodd}
      />

<AddSenderIdModal
        isModalOpen={isModalOpenAdd}
        setIsModalOpen={setIsModalOpenAdd}
        prodd={prodd}
      />
    </div>
  );
}

export default SenderList;
