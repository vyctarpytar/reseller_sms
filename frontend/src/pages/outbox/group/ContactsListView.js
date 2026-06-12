import { Dropdown, Input, Modal, Table } from "antd";
import moment from "moment";
import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
  deleteContact,
  fetchContacts,
  setGradUpload,
  setGraduateObj,
} from "../../../features/contact/ContactSlice";
import { Link, useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { dateForHumans } from "../../../utils";
import SmsGroupModal from "./SmsGroupModal";
import DeleteModal from "../../../components/DeleteModal";
import svg27 from '../../../assets/svg/svg27.svg'

export default function ContactsListView({ handleGraduateOption }) {
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const { gradLoading, gradListObj, contacts,deleteLoading } = useSelector(
    (state) => state.contact
  );

  const [activeGraduate, setactiveGraduate] = useState({}); 
  const { folderObj } = useSelector((state) => state.folder);

  const { saving } = useSelector((state) => state.save);
  const [search, setsearch] = useState("");

  const filteredContacts = search
    ? contacts?.filter(
        (item) =>
          item?.chFirstName?.toLowerCase().includes(search.toLowerCase()) ||
          item?.chMemberNo?.toLowerCase().includes(search.toLowerCase()) ||
          item?.chNationalId?.toLowerCase().includes(search.toLowerCase()) ||
          item?.chGenderCode?.toLowerCase().includes(search.toLowerCase())
      )
    : contacts;

  const [prodd, setProdd] = useState(""); 
  const [isModalOpenDelete, setIsModalOpenDelete] = useState(false);
  const showModalDelete = async () => {
    setIsModalOpenDelete(true);
  };
  const handleDelete = async () => {
    const res = await dispatch(
      deleteContact(prodd?.chId)
    ); 
    if (res?.payload?.success) {
      await toast.success(res?.payload?.messages?.message);
      handlefetchContactsData();
      await setIsModalOpenDelete(false);
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  function handleGoBack() {
    navigate(-1);
  }

  async function handleActiveGraduate(item) {
    await dispatch(setGraduateObj(item));
    await setactiveGraduate(item);
  }

  async function handleUploadFile() {
    await dispatch(setGradUpload(true));
    await navigate("/contacts/add/file-upload");
  }

  async function handlefetchContactsData() {
    await dispatch(fetchContacts(folderObj?.groupId));
  }

  const [selectedRowKeys, setSelectedRowKeys] = useState([]);

  const [rowId, setRowId] = useState([]);

  const handleEmployeeToReturns = async (selectedRows) => {
    setRowId(selectedRows);
  };

  const onSelectChange = (keys, rows) => {
    setSelectedRowKeys(keys);
    handleEmployeeToReturns(rows);
  };

  const handleSelectTemplate = async () => {
    navigate("/templates");
  };

  const rowSelection = {
    selectedRowKeys,
    onChange: onSelectChange,
  };
  const [isModalOpen, setIsModalOpen] = useState(false);

  const showModal = async () => {
    if (contacts?.length === 0) {
      toast.error("Please add members");
      return;
    }
    setIsModalOpen(true);
  };
  const handleEdit = ()=>{
    navigate(`/contact-edit/${prodd?.chId}`)
  }

  const settingItems = [
    {
      key: "0",
      label: (
        <Link
          onClick={handleEdit}
          className="flex  text-[16px] font-sans  !text-darkGreen"
        >
          Edit
        </Link>
      ),
    },

    {
      key: "1",
      label: (
        <div
          onClick={showModalDelete}
          className="flex  text-[16px] font-sans  !text-darkGreen"
        >
          Remove
        </div>
      ),
    },
  ];

  const settingItemsQuickActions = [
    {
      key: "0",
      label: (
        <div
          onClick={handleGraduateOption}
          className="font-dmSans text-[#344054] font-[500] text-[18px] !mt-[5%]"
        >
          Add contacts
        </div>
      ),
    },

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
          onClick={handleSelectTemplate}
          className="font-dmSans text-[#344054] font-[500] text-[18px] !mt-[5%]"
        >
          Select template
        </div>
      ),
    },
  ];

  const columns = [
    {
      title: "ID No.",
      dataIndex: "chNationalId",
      sorter: (a, b) => a.chNationalId.localeCompare(b.chNationalId),
    },

    {
      title: "First Name of member",
      render: (item) => <span>{item?.chFirstName}</span>,
      sorter: (a, b) => a.chFirstName.localeCompare(b.chFirstName),
    },
    {
      title: "Other Names of member",
      render: (item) => <span>{item?.chOtherName}</span>,
      sorter: (a, b) => a.chOtherName.localeCompare(b.chOtherName),
    },
    {
      title: "Phone No.",
      dataIndex: "chTelephone",
      sorter: (a, b) => a.chTelephone.localeCompare(b.chTelephone),
    },
    {
      title: "Date of Birth",
      render: (item) => {
        return <div>{dateForHumans(item)}</div>;
      },
      dataIndex: "chDob",
      sorter: (a, b) => a.chDob.localeCompare(b.chDob),
    },
    {
      title: "Gender",
      dataIndex: "chGenderCode",
      sorter: (a, b) => a.chGenderCode.localeCompare(b.chGenderCode),
    },
    {
      title: "Option 1",
      render: (item) => <span>{item?.chOption1}</span>,
      sorter: (a, b) => a.chOption1.localeCompare(b.chOption1),
    },
    {
      title: "Option 2",
      render: (item) => <span>{item?.chOption2}</span>,
      sorter: (a, b) => a.chOption2.localeCompare(b.chOption2),
    },

    {
      title: "Option 3",
      render: (item) => <span>{item?.chOption3}</span>,
      sorter: (a, b) => a.chOption3.localeCompare(b.chOption3),
    },
    {
      title: "Option 4",
      render: (item) => <span>{item?.chOption4}</span>,
      sorter: (a, b) => a.chOption4.localeCompare(b.chOption4),
    },  

    {
      title: "",
      render: (item) => (
        <>
          <Dropdown
            onOpenChange={() => handleActiveGraduate(item)}
            overlayStyle={{
              width: "200px",
            }}
            trigger={"click"}
            menu={{ items: settingItems }}
            placement="bottom"
          >
            <button onClick={() => setProdd(item)}>
            <img src={svg27} alt="svg27" />
            </button>
          </Dropdown>
        </>
      ),
    },
  ];

  useEffect(() => {
    handlefetchContactsData();
  }, []);

  return (
    <>
      <div className="mt-[35px]">
        <div className="flex lg:flex-row flex-col gap-y-5 justify-between lg:items-center items-start w-[100%]">
          <div className="flex items-center lg:w-[50%] w-full">
            <Input
              value={search}
              onChange={(e) => setsearch(e.target.value)}
              placeholder="Search member name or keyword"
              className="text-[16px] font-[400] flex-row-reverse w-[100%]"
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
        </div>

        <div className="text-black1 mt-[45px] mb-[21px] text-[20px] font-medium leading-[24px]">
          List of members
        </div>

        <Table
          rowSelection={false}
          loading={gradLoading}
          className="w-full"
          columns={columns}
          dataSource={filteredContacts}
          scroll={{
            x: 1100,
          }}
          rowKey={(record) => record?.chId}
        />
      </div>

      <SmsGroupModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
      />

      <DeleteModal
        isModalOpen={isModalOpenDelete}
        setIsModalOpen={setIsModalOpenDelete}
        prodd={prodd}
        handleDelete={handleDelete}
        loading={deleteLoading}
        content={`Are you sure you want to delete ${prodd?.chFirstName}?`}
        title={`Remove ${prodd?.chFirstName}`}
      />
    </>
  );
}
