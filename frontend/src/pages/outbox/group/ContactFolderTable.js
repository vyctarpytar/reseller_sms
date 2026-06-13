import { Dropdown, Input } from "antd";
import moment from "moment";
import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Link, useNavigate } from "react-router-dom";
import {
  deleteGroup,
  setFolderObj,
} from "../../../features/folder/folderSlice";
import DeleteModal from "../../../components/DeleteModal";
import toast from "react-hot-toast";
import AddFolderModal from "./modal/AddFolderModal";
import svg27 from "../../../assets/svg/svg27.svg";
import SmsGroupModal from "./SmsGroupModal";
import SmsManyGroupModal from "./SmsManyGroupModal";
import ConfirmModal from "../../../components/ConfirmModal";
import { deleteRequest } from "../../../features/save/saveSlice";
import ResponsiveTable, { hideBelow } from "../../../components/ResponsiveTable";

export default function ContactFolderTable({
  handleFetchData,
  handleAddFolder,
}) {
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const { fldLoading, gradFolders, deleteLoading } = useSelector(
    (state) => state.folder
  );
  const {saving } =  useSelector((state)=>state.save)

  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [hasSelected, sethasSelected] = useState(false);
  const [activeFolder, setactiveFolder] = useState({});
  const [search, setsearch] = useState("");

  const [open, setopen] = useState(false);
  const [openDelete, setOpenDelete] = useState(false); 
  const handleOpenDelete = async (item) => { 
    await setOpenDelete(true);
  };
  const handleCloseDelete = () => {
    setOpenDelete(false);
  };

  

  function handleCancel() {
    setopen(false);
  }

  const filterDataList = gradFolders?.filter((item) => {
    if (search.length) {
      return item?.groupName?.toUpperCase().includes(search.toUpperCase());
    }
    return item;
  });

  async function handleFolderChange(item) {
    await dispatch(setFolderObj(item));
    await setactiveFolder(item);
    await navigate(`/contacts/folder/${item?.groupName}`);
  }

  const [prodd1, setProdd1] = useState("");
  const [isModalOpenDelete, setIsModalOpenDelete] = useState(false);
  const showModalDelete = async () => {
    setIsModalOpenDelete(true);
  };
  const handleDelete = async () => {
    const res = await dispatch(deleteGroup(prodd1?.groupId));
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      handleFetch();
      await setIsModalOpenDelete(false);
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  const handleView = () => {
    handleFolderChange(prodd1);
  };

  const handleEdit = () => {
    setopen(true);
  };

  const settingItems = [
    {
      key: "0",
      label: (
        <Link
          onClick={handleView}
          className="flex  text-[16px] font-sans   !text-primary"
        >
          View
        </Link>
      ),
    },

    {
      key: "1",
      label: (
        <Link
          onClick={handleEdit}
          className="flex  text-[16px] font-sans  !text-primary"
        >
          Edit
        </Link>
      ),
    },

    {
      key: "2",
      label: (
        <div
          onClick={showModalDelete}
          className="flex  text-[16px] font-sans   !text-primary"
        >
          Remove
        </div>
      ),
    },
  ];

  const columns = [
    {
      title: "Group name",
      sorter: (a, b) => a.groupName.localeCompare(b.groupName),
      render: (item) => (
        <div
          className="cursor-pointer"
          onClick={() => handleFolderChange(item)}
        >
          {item?.groupName}
        </div>
      ),
    },
    {
      title: "Group description",
      dataIndex: "groupDescription",
      ...hideBelow(),
      sorter: (a, b) => a.groupDescription.localeCompare(b.groupDescription),
    },
    {
      title: "Creator",
      dataIndex: "groupCreatedByName",
      ...hideBelow(),
      sorter: (a, b) =>
        a.groupCreatedByName.localeCompare(b.groupCreatedByName),
    },
    {
      title: "Date Created",
      dataIndex: "groupCreationDate",
      ...hideBelow(),
      sorter: (a, b) => a.groupCreationDate.localeCompare(b.groupCreationDate),
      render: (item) => <span>{moment(item).format("Do MMMM YYYY")}</span>,
    },
    {
      title: "Actions",
      render: (item) => (
        <>
          <Dropdown
            overlayStyle={{
              width: "250px",
            }}
            trigger={"click"}
            menu={{ items: settingItems }}
            placement="bottom"
          >
            <button onClick={() => setProdd1(item)}>
              <img src={svg27} alt="svg27" />
            </button>
          </Dropdown>
        </>
      ),
    },
    // {
    //   title: "",
    //   render: (item) => (
    //     <>
    //       <button
    //         className="text-blueDark"
    //         onClick={() => handleFolderChange(item)}
    //       >
    //         view
    //       </button>
    //     </>
    //   ),
    // },
  ];

  const handleSelectTemplate = async () => {
    navigate("/templates");
  };

  const [isModalOpen, setIsModalOpen] = useState(false);

  const showModal = async () => {
    // if (contacts?.length === 0) {
    //   toast.error("Please add members");
    //   return;
    // }
    setIsModalOpen(true);
  };

  const settingItemsQuickActions = [
    {
      key: "1",
      label: (
        <div
          onClick={showModal}
          className="font-dmSans text-[#344054] font-[500] text-[18px] !mt-[5%]"
        >
          Send sms
        </div>
      ),
    },
 {
      key: "2",
      label: (
        <div
          onClick={handleOpenDelete}
          className="font-dmSans text-[#344054] font-[500] text-[18px] !mt-[5%]"
        >
       Delete groups
        </div>
      ),
    },

  ];

  const [rowId, setRowId] = useState([]); 
  const handleEmployeeToReturns = async (selectedRows) => {
    await setRowId(selectedRows);
    // await showModalGroup();
  };

  const onSelectChange = (keys, rows) => {
    setSelectedRowKeys(keys);
    handleEmployeeToReturns(rows);
  };

  const rowSelection = {
    selectedRowKeys,
    onChange: onSelectChange,
  };

  const handleDeleteMultiplegroups=async()=>{
     const groupIds = rowId?.map((g) => g?.groupId); 
  const res = await dispatch(
      deleteRequest({
        url: `api/v2/groups/multiple`,
        groupId:groupIds
      })
    );
    if (res?.payload?.success) {
      toast.success(res?.payload?.messages?.message);
      await handleFetch();
      await handleCloseDelete(); 
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  }

  async function handleFetch() {
    await handleFetchData();
  }

  useEffect(() => {}, [gradFolders, activeFolder]);

  useEffect(() => {
    handleFetch();
  }, []);

  return (
    <>
      <div className="flex flex-col  w-full">
        <h3 className="font-[700] text-[24px] text-black1 dash-title ">
          Contact groups
        </h3>
        <div className="mt-5 flex lg:flex-row flex-col gap-y-5 justify-between lg:items-center items-start w-full">
          <div className="flex items-center lg:w-[30%] w-full">
            <Input
              value={search}
              onChange={(e) => setsearch(e.target.value)}
              placeholder="Search groups name or keyword"
              className="text-[16px] font-[400] flex-row-reverse"
              prefix={
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="24"
                  height="24"
                  viewBox="0 0 24 24"
                  fill="none"
                >
                  <path
                    fill-rule="evenodd"
                    clip-rule="evenodd"
                    d="M11.2508 3.75C7.50609 3.75 4.47041 6.93997 4.47041 10.875C4.47041 14.81 7.50609 18 11.2508 18C14.9955 18 18.0312 14.81 18.0312 10.875C18.0312 6.93997 14.9955 3.75 11.2508 3.75ZM3.04297 10.875C3.04297 6.11154 6.71773 2.25 11.2508 2.25C15.7838 2.25 19.4586 6.11154 19.4586 10.875C19.4586 15.6385 15.7838 19.5 11.2508 19.5C6.71773 19.5 3.04297 15.6385 3.04297 10.875Z"
                    fill="#333333"
                  />
                  <path
                    fill-rule="evenodd"
                    clip-rule="evenodd"
                    d="M16.045 15.913C16.3237 15.6201 16.7756 15.6201 17.0543 15.913L21.3902 20.4693C21.6689 20.7622 21.6689 21.237 21.3902 21.5299C21.1115 21.8228 20.6596 21.8228 20.3809 21.5299L16.045 16.9737C15.7663 16.6808 15.7663 16.2059 16.045 15.913Z"
                    fill="#333333"
                  />
                </svg>
              }
            />
          </div>
          <div className="flex lg:flex-row flex-col lg:items-center items-start lg:gap-x-10 gap-y-5">
            {rowId && rowId?.length > 0 && (
              <Dropdown
                overlayStyle={{
                  width: "250px",
                }}
                trigger={"click"}
                menu={{ items: settingItemsQuickActions }}
                placement="bottom"
              >
                <div className="w-[250px]">
                  <button className="cstm-btn" type="submit">
                    Quick actions
                  </button>
                </div>
              </Dropdown>
            )}

            <div className="w-[229px]">
              <button onClick={() => handleAddFolder()} className="cstm-btn">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="25"
                  height="24"
                  viewBox="0 0 25 24"
                  fill="none"
                >
                  <path
                    d="M19.5 11H13.5V5C13.5 4.73478 13.3946 4.48043 13.2071 4.29289C13.0196 4.10536 12.7652 4 12.5 4C12.2348 4 11.9804 4.10536 11.7929 4.29289C11.6054 4.48043 11.5 4.73478 11.5 5V11H5.5C5.23478 11 4.98043 11.1054 4.79289 11.2929C4.60536 11.4804 4.5 11.7348 4.5 12C4.5 12.2652 4.60536 12.5196 4.79289 12.7071C4.98043 12.8946 5.23478 13 5.5 13H11.5V19C11.5 19.2652 11.6054 19.5196 11.7929 19.7071C11.9804 19.8946 12.2348 20 12.5 20C12.7652 20 13.0196 19.8946 13.2071 19.7071C13.3946 19.5196 13.5 19.2652 13.5 19V13H19.5C19.7652 13 20.0196 12.8946 20.2071 12.7071C20.3946 12.5196 20.5 12.2652 20.5 12C20.5 11.7348 20.3946 11.4804 20.2071 11.2929C20.0196 11.1054 19.7652 11 19.5 11Z"
                    fill="#EDF8FF"
                  />
                </svg>
                <span className="ml-1">Add contact group</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="dash-inner-page mt-[24px] max-w-full w-full overflow-x-auto">
        <section>
          <ResponsiveTable
            rowSelection={rowSelection}
            className="mt-[1.31rem] w-full"
            scroll={{
              x: 800,
            }}
            rowKey={(record) => record?.groupId}
            columns={columns}
            dataSource={filterDataList}
            loading={fldLoading}
          />
        </section>
      </div>

      <DeleteModal
        isModalOpen={isModalOpenDelete}
        setIsModalOpen={setIsModalOpenDelete}
        prodd={prodd1}
        handleDelete={handleDelete}
        loading={deleteLoading}
        content={`Are you sure you want to delete ${prodd1?.groupName}?`}
        title={`Remove ${prodd1?.groupName}`}
      />
        <ConfirmModal
        open={openDelete}
        handleCancel={handleCloseDelete}
        handleSubmit={handleDeleteMultiplegroups}
        loading={saving}
        content={`Are you sure you want to delete ${rowId?.length} groups?`}
        type="alert"
        btnTitle="Delete"
      />

      <AddFolderModal
        handleFetchData={handleFetchData}
        open={open}
        handleCancel={handleCancel}
        prodd={prodd1}
      />

      {/* <SmsGroupModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        rowIdTableGroup={rowId}
      /> */}

      <SmsManyGroupModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        rowIdTableGroup={rowId}
      />
    </>
  );
}
