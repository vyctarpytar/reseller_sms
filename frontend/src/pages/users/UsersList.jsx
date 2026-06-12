import React, { useEffect, useState } from "react";
import UsersAddModal from "./UsersAddModal";
import InsideHeader from "../../components/InsideHeader";
import { Dropdown, Skeleton, Table, Tag } from "antd";
import MaterialIcon from "material-icons-react";
import { useDispatch, useSelector } from "react-redux";
import {
  fetchMyProfile,
  fetchMyUsers,
  fetchPermissions,
} from "../../features/auth/authSlice";
import svg38 from "../../assets/svg/svg38.svg";
import { useNavigate } from "react-router-dom";
import { addSpaces, dateForHumans, getLetterWord } from "../../utils";
import svg27 from "../../assets/svg/svg27.svg";
import UserPermissionModal from "./UserPermissionModal";
import FilterModal from "./FilterModal";

function UsersList() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };

  const [isModalOpenFilter, setIsModalOpenFilter] = useState(false);
  const showModalFilter = () => {
    setIsModalOpenFilter(true);
  };
  const [formData, setFormData] = useState({});

  const [isModalOpenPermission, setIsModalOpenPermission] = useState(false);
  const showModalPermission = () => {
    setIsModalOpenPermission(true);
  };

  const [prodd, setProdd] = useState();
  const { myUsersData, usersLoading, userProfile, loading, user, usersCount } =
    useSelector((state) => state.auth);

  const dispatch = useDispatch();
  const navigate = useNavigate();

  async function fetchPermissionsData() {
    await dispatch(
      fetchPermissions({
        role: prodd?.role,
      })
    );
  }

  const hasPermissions = myUsersData?.some(
    (item) => item?.permissions?.length > 0
  );

  const colors = ["#FF5733", "#33FF57", "#3357FF", "#FF33A5", "#A533FF"];

  const columns = [
    {
      title: "Full Name",
      render: (item) => {
        return (
          <div className="flex  items-center gap-x-5">
            <div className="w-[50px] h-[50px] rounded-full flex items-center justify-center bg-lightBlue text-blue">
              {item?.usrLogo ? (
                <img src={item?.usrLogo} alt="logo" />
              ) : (
                getLetterWord(`${item?.firstname} ${item?.lastname}`)
              )}
            </div>
            {item?.firstname} {item?.lastname}
          </div>
        );
      },
    },
    {
      title: "Phone Number",
      render: (item) => {
        return <div>{addSpaces(item)}</div>;
      },
      dataIndex: "phoneNumber",
    },
    {
      title: "National ID",
      dataIndex: "usrNationalId",
    },
    {
      title: "Status",
      render: (item) => {
        return (
          <div className="flex items-center text-center gap-x-2">
            <span
              className={`w-[10px] h-[10px] rounded-full ${
                item?.usrStatus === "ACTIVE" ? "bg-green" : "bg-red"
              }`}
            ></span>
            {item?.usrStatus}
          </div>
        );
      },
    },
    {
      title: "Role",
      dataIndex: "role",
    },

    ...(hasPermissions
      ? [
          {
            title: "Permission",
            render: (item) => {
              return (
                <div className="flex flex-col">
                  {item?.permissions?.map((permission, index) => {
                    const randomColor =
                      colors[Math.floor(Math.random() * colors.length)];
                    return (
                      <div
                        key={index}
                        className="flex items-center gap-x-1 mb-1"
                      >
                        <span
                          className="w-[10px] h-[10px] rounded-full"
                          style={{ backgroundColor: randomColor }}
                        ></span>
                        <div>{permission}</div>
                      </div>
                    );
                  })}
                </div>
              );
            },
          },
        ]
      : []),
    {
      title: "Created Date",
      render: (item) => {
        return <div>{dateForHumans(item)}</div>;
      },
      dataIndex: "createdDate",
    },

    {
      title: "Actions",
      render: (item) => (
        <>
          <button onClick={() => setProdd(item)}>
            <Dropdown
              overlayStyle={{
                width: "200px",
              }}
              trigger={"click"}
              menu={{ items: settingItems }}
              placement="bottom"
            >
              <img src={svg27} alt="svg27" />
            </Dropdown>
          </button>
        </>
      ),
    },
  ];
  const handleEdit = async (item) => {
    await showModal();
  };

  const handlePermissions = async (item) => {
    await fetchPermissionsData();
    await showModalPermission();
  };

  const handleAdd = async (item) => {
    await setProdd("");
    await showModal();
  };

  const settingItems = [
    {
      key: "0",
      label: (
        <div
          className=" flex  text-[16px] font-sans items-center justify-center text-darkGreen"
          onClick={handleEdit}
        >
          Edit
        </div>
      ),
    },
    {
      key: "1",
      label: (
        <div
          className=" mb-1 flex text-[16px] font-sans items-center justify-center  text-darkGreen"
          onClick={handlePermissions}
        >
          Assign Permission
        </div>
      ),
    },
  ];
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const handleClearFilters = async () => {
    await setFormData({});
    const res = await dispatch(
      fetchMyUsers({
        url: "api/v2/users/get",
        firstname: null,
        lastname: null,
        email: null,
        phoneNumber: null,
        usrNationalId: null,
        usrStatus: null,
        role: null,
      })
    );
  };

  async function fetchMyUsersData(page, size) {
    const res = await dispatch(
      fetchMyUsers({
        url: "api/v2/users/get",
        limit: size ?? pageSize,
        start: page ?? pageIndex,
        firstname: formData?.firstname,
        lastname: formData?.lastname,
        email: formData?.email,
        phoneNumber: formData?.phoneNumber,
        usrNationalId: formData?.usrNationalId,
        usrStatus: formData?.usrStatus,
        role: formData?.role,
      })
    );
  }

  async function fetchProfileData() {
    await dispatch(fetchMyProfile());
  }

  useEffect(() => {
    fetchMyUsersData();
    fetchProfileData();
  }, []);

  
  useEffect(() => {
    if (user?.role != "ADMIN" && user?.role != "SUPER_ADMIN") {
      navigate("/roles-error-page");
    }
  }, []);
 

  return (
    <div className="w-full h-full overflow-y-scroll">
      <InsideHeader
        title="List of Users"
        subtitle="Manage your users here"
        back={true}
      />

      <div className="lg:px-10 px-3">
        <div className="flex  items-center gap-x-10  mt-10">
          <div className="w-[140px]">
            <button
              className="cstm-btn !rounded-[4px] !bg-[#A3A2A7] !text-[.75rem] flex items-center gap-x-3"
              onClick={handleAdd}
            >
              Add User
            </button>
          </div>
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
        <Table
          className="mt-[1.31rem] w-full"
          scroll={{
            x: 800,
          }}
          pagination={{
            position: ["bottomCenter"],
            current: pageIndex + 1,
            total: usersCount,
            pageSize: pageSize,
            onChange: (page, size) => {
              setPageIndex(page - 1);
              setPageSize(size);
              fetchMyUsersData(page - 1, size);
            },
            showSizeChanger: false,
            hideOnSinglePage: true,
          }}
          rowKey={(record) => record?.usrId}
          columns={columns}
          dataSource={myUsersData}
          loading={usersLoading}
        />
      </div>

      <UsersAddModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        prodd={prodd}
      />

      <UserPermissionModal
        isModalOpen={isModalOpenPermission}
        setIsModalOpen={setIsModalOpenPermission}
        prodd={prodd}
      />

      <FilterModal
        isModalOpen={isModalOpenFilter}
        setIsModalOpen={setIsModalOpenFilter}
        formData={formData}
        setFormData={setFormData}
      />
    </div>
  );
}

export default UsersList;
