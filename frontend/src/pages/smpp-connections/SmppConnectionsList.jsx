import React, { useEffect, useState } from "react";
import { Skeleton } from "antd";
import { useDispatch, useSelector } from "react-redux";
import toast from "react-hot-toast";
import InsideHeader from "../../components/InsideHeader";
import ResponsiveTable, { hideBelow } from "../../components/ResponsiveTable";
import StatusBadge from "../../components/StatusBadge";
import ConfirmModal from "../../components/ConfirmModal";
import svg25 from "../../assets/svg/svg25.svg";
import { save } from "../../features/save/saveSlice";
import { fetchSmppConnections } from "../../features/smpp/smppSlice";
import { fetchResellerAccounts } from "../../features/reseller-account/resellerAccountSlice";
import { dateForHumans } from "../../utils";
import SmppConnectionModal from "./SmppConnectionModal";
import { connectionDetailsText, isUpstream } from "./smppCredentials";

// "DISABLED", not "INACTIVE": StatusBadge matches on substrings and would paint
// anything containing ACTIVE green.
const statusOf = (item) => (item?.isActive ? "ACTIVE" : "DISABLED");

function SmppConnectionsList() {
  const dispatch = useDispatch();
  const { smppConnections, loading } = useSelector((state) => state.smpp);
  const { saving } = useSelector((state) => state.save);

  const [prodd, setProdd] = useState();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [openToggle, setOpenToggle] = useState(false);

  const handleAdd = () => {
    setProdd(null);
    setIsModalOpen(true);
  };

  const handleEdit = (item) => {
    setProdd(item);
    setIsModalOpen(true);
  };

  const handleAskToggle = (item) => {
    setProdd(item);
    setOpenToggle(true);
  };

  const handleCopy = async (item) => {
    try {
      await navigator.clipboard.writeText(connectionDetailsText(item));
      toast.success("Connection details copied");
    } catch (e) {
      toast.error("Could not copy. Copy the details from the table instead.");
    }
  };

  const handleToggle = async () => {
    const res = await dispatch(
      save({
        url: "api/v2/smpp-connection",
        id: prodd?.id,
        isActive: !prodd?.isActive,
      })
    );
    if (res?.payload?.success) {
      toast.success(res?.payload?.messages?.message);
      setOpenToggle(false);
      dispatch(fetchSmppConnections());
      dispatch(fetchResellerAccounts());
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  const actions = (item) => (
    <div className="flex gap-x-4 whitespace-nowrap">
      {!isUpstream(item) && (
        <button className="text-primary" onClick={() => handleCopy(item)}>
          Copy details
        </button>
      )}
      <button className="text-primary" onClick={() => handleEdit(item)}>
        Edit
      </button>
      <button className="text-primary" onClick={() => handleAskToggle(item)}>
        {item?.isActive ? "Deactivate" : "Activate"}
      </button>
    </div>
  );

  const columns = [
    {
      title: "Account",
      dataIndex: "accName",
      render: (accName, item) => (
        <div>
          <div>{accName ?? "All accounts"}</div>
          {item?.connectionName && (
            <div className="text-[11px] text-muted">{item.connectionName}</div>
          )}
        </div>
      ),
    },
    {
      title: "Direction",
      ...hideBelow(),
      render: (item) => (
        <span className="badge-neutral whitespace-nowrap">
          {isUpstream(item) ? "Upstream (we connect)" : "Client connects to us"}
        </span>
      ),
    },
    {
      title: "Username",
      dataIndex: "systemId",
    },
    {
      title: "Password",
      dataIndex: "password",
    },
    {
      title: "Host",
      ...hideBelow(),
      render: (item) => `${item?.host}:${item?.port}`,
    },
    {
      title: "Bind mode",
      ...hideBelow(),
      dataIndex: "bindType",
    },
    {
      title: "Created",
      ...hideBelow(),
      dataIndex: "createdAt",
      render: (item) => dateForHumans(item),
    },
    {
      title: "Status",
      render: (item) => <StatusBadge value={statusOf(item)} />,
    },
    {
      title: "Actions",
      render: (item) => actions(item),
    },
  ];

  useEffect(() => {
    dispatch(fetchSmppConnections());
    dispatch(fetchResellerAccounts());
  }, []);

  return (
    <>
      <div className="w-full overflow-y-scroll h-full">
        <InsideHeader
          title="SMPP Connections"
          subtitle="Issue SMPP credentials to accounts that connect over SMPP"
          back={false}
        />

        <div className="lg:px-10 px-3">
          <div className="mt-10 flex lg:flex-row flex-col justify-between gap-y-4">
            <div className="flex flex-col">
              <div className="product_request_title !text-[31px]">
                SMPP Connections
              </div>
              <div className="product_sub mt-[0.5rem]">
                An account can use SMPP and its API key side by side. Both bill
                the same balance.
              </div>
            </div>
            <div className="w-[230px]">
              <button
                className="cstm-btn flex items-center gap-x-3"
                onClick={handleAdd}
              >
                <img src={svg25} alt="" />
                Add SMPP connection
              </button>
            </div>
          </div>

          {loading && !smppConnections?.length ? (
            <Skeleton className="mt-[1.31rem]" />
          ) : (
            <ResponsiveTable
              className="mt-[1.31rem] w-full mb-10"
              scroll={{ x: 900 }}
              pagination={{
                position: ["bottomCenter"],
                pageSize: 10,
                showSizeChanger: false,
                hideOnSinglePage: true,
              }}
              rowKey={(record) => record?.id}
              columns={columns}
              dataSource={smppConnections}
              loading={loading}
              mobileEmptyText="No SMPP connections yet"
              mobileCard={(record) => (
                <div className="card !p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="font-medium truncate">
                        {record?.accName ?? "All accounts"}
                        {isUpstream(record) ? " · Upstream" : ""}
                      </p>
                      <p className="text-[11px] text-muted mt-1.5 break-all">
                        {record?.systemId} · {record?.password}
                      </p>
                      <p className="text-[11px] text-muted mt-1 truncate">
                        {record?.host}:{record?.port} · {record?.bindType}
                      </p>
                    </div>
                    <StatusBadge value={statusOf(record)} />
                  </div>
                  <div className="mt-3">{actions(record)}</div>
                </div>
              )}
            />
          )}
        </div>
      </div>

      <SmppConnectionModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        prodd={prodd}
      />

      <ConfirmModal
        open={openToggle}
        handleCancel={() => setOpenToggle(false)}
        handleSubmit={handleToggle}
        loading={saving}
        content={
          prodd?.isActive
            ? `Deactivate ${prodd?.systemId}? New binds will be refused. A session that is already connected stays up until it disconnects.`
            : `Activate ${prodd?.systemId}? It will be able to bind again.`
        }
        type="info"
        btnTitle={prodd?.isActive ? "Deactivate" : "Activate"}
      />
    </>
  );
}

export default SmppConnectionsList;
