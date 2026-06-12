import { Table } from "antd";
import React, { useState } from "react";
import InsideHeader from "../../components/InsideHeader";
import { useSelector } from "react-redux";
import ConfirmSubmitModal from "../../components/ConfirmSubmitModal";

function ApproveSms() {
  const [prodd, setProdd] = useState();
  const { saving } = useSelector((state) => state.save);
  const dataSource = [
    {
      key: "1",
      name: "Mike",
      age: 32,
      address: "10 Downing Street",
    },
    {
      key: "2",
      name: "John",
      age: 42,
      address: "10 Downing Street",
    },
  ];

  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = () => {
    setIsModalOpen(true);
  };

  const handleSubmit = () => {};

  const columns = [
    {
      title: "Name",
      dataIndex: "name",
      key: "name",
    },
    {
      title: "Age",
      dataIndex: "age",
      key: "age",
    },
    {
      title: "Address",
      dataIndex: "address",
      key: "address",
    },
    {
      title: "Actions",
      render: (item) => (
        <>
          <button onClick={() => setProdd(item)}>
            <div className="text-darkGreen" onClick={showModal}>
              View
            </div>
          </button>
        </>
      ),
    },
  ];

  return (
    <>
      <div className="w-full overflow-y-scroll h-full">
        <InsideHeader
          title="Group Broadcasts"
          subtitle="Manage your group broadcasts messages before they are sent"
          back={true}
        />
        <Table
          className="mt-[1.31rem] w-full mb-10"
          scroll={{
            x: 800,
          }}
          pagination={{
            position: ["bottomCenter"],
            current: pageIndex + 1,
            // total: sentSmsCount,
            pageSize: pageSize,
            onChange: (page, size) => {
              setPageIndex(page - 1);
              setPageSize(size);
              // fetchSentSmsData(page - 1, size);
            },
            showSizeChanger: false,
            hideOnSinglePage: true,
          }}
          //   rowKey={(record) => record?.msgId}
          columns={columns}
          dataSource={dataSource}
          //   loading={loadingSms}
        />
      </div>

      <ConfirmSubmitModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        content="Are you sure?"
        subContent={`Are you sure that you want to approve ${prodd?.accName}`}
        handleSubmit={handleSubmit}
        loading={saving}
      />
    </>
  );
}

export default ApproveSms;
