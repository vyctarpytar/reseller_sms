import { Dropdown, Table } from "antd";
import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { dateForHumans } from "../../utils"; 
import { useNavigate } from "react-router-dom";
import DeleteModal from "../../components/DeleteModal";
import MaterialIcon from "material-icons-react";
import { save } from "../../features/save/saveSlice";
import toast from "react-hot-toast";
import { fetchResellerAccounts } from "../../features/reseller-account/resellerAccountSlice";
import FilterModal from "./FilterModal";
import svg38 from "../../assets/svg/svg38.svg";

function AccountAdminTable() { 
  const { refetchKey } = useSelector((state) => state.resellerCodes);
  const { saving } = useSelector((state) => state.save);
  const { resellerAccountData } = useSelector((state) => state.resellerAccount);
  
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({});
  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };
 
  const [prodd, setProdd] = useState("");

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
      title: "City",
      dataIndex: "accCity", 
    },
    {
      title: "Country",
      dataIndex: "accCountry", 
    }, 
    {
      title: "Created Date",
      render: (item) => {
        return <div>{dateForHumans(item)}</div>;
      },
      dataIndex: "accCreatedDate", 
    },
    {
      title: "Status",
      render: (item) => {
        return (
          <div className="flex items-center text-center gap-x-2">
            <span
              className={`w-[10px] h-[10px] rounded-full ${
                item?.accStatus === "ACTIVE" ? "bg-green" : "bg-red"
              }`}
            ></span>
            {item?.accStatus}
          </div>
        );
      }, 
    }, 
   
    {
      title: "Actions",
      render: (item) => (
        <>
          <button onClick={()=>setProdd(item)}>
            <div className="text-darkGreen" onClick={()=>handleAccountView(item)}>View</div>
            {/* <Dropdown
              overlayStyle={{
                width: "150px",
              }}
              trigger={"click"}
              menu={{ items: settingItems }}
              placement="bottom"
            >
              <img src={svg27} alt="svg27"/> 
            </Dropdown> */}
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

  function handleAccountView(item){
    navigate(`/account-admin-view/${item?.accId}`)
  }

  const settingItems = [
    {
      key: "0",
      label: (
        <div className=" mb-1 flex text-[16px] font-sans items-center justify-center  text-darkGreen"
        onClick={handleAccountView}>
          View
        </div>
      ),
       
    },
    {
      key: "1",
      label: (
        <div
          className=" flex  text-[16px] font-sans items-center justify-center text-darkGreen" 
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
      fetchResellerAccountData(); 
      await setIsModalOpenDelete(true);
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  } 
 
  function fetchResellerAccountData() {
    dispatch(fetchResellerAccounts());
  }

  const handleClearFilters = async (page, size) => { 
    await setFormData({}); 
   await fetchResellerAccountData()
  };

  useEffect(()=>{
    fetchResellerAccountData()
  },[])

 
  return (
    <div className="mt-10 lg:px-10 px-3">
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
      <Table
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
        rowKey={(record) => record?.accId}
        columns={columns}
        dataSource={resellerAccountData}
      />

      <DeleteModal
        isModalOpen={isModalOpenDelete}
        setIsModalOpen={setIsModalOpenDelete}
        prodd={prodd}
        handleDelete={handleDelete}
        loading={saving}
        content={`Are you sure you want to disable ${prodd?.accName}?`}
        title={`Disable ${prodd?.accName}`}
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

export default AccountAdminTable;
