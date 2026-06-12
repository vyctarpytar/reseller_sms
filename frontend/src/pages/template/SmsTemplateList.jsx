import { Skeleton, Table } from "antd";
import React, { useEffect, useState } from "react";
import InsideHeader from "../../components/InsideHeader";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import svg32 from "../../assets/svg/svg32.svg";
import noCon from "../../assets/img/Container2.png";
import TemplateModal from "./TemplateModal";
import MaterialIcon from "material-icons-react";
import SmsGroupModal from "../outbox/group/SmsGroupModal";
import svg25 from "../../assets/svg/svg25.svg";
import { fetchTemplates } from "../../features/save/saveSlice";
import { dateForHumans } from "../../utils";

function SmsTemplateList() {
  const { user } = useSelector((state) => state.auth);
  const { sentSmsData, loadingSms, templateCount, templatesData, loading } =
    useSelector((state) => state.save);

  const [formData, setFormData] = useState({});

  const [prodd, setProdd] = useState();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const showModal = async() => {
   await setIsModalOpen(true);
   await setProdd('')
  };

  const showModalEdit = async() => {
    await setIsModalOpen(true);
   };

  const [isModalOpenGroup, setIsModalOpenGroup] = useState(false);
  const showModalGroup = () => {
    setIsModalOpenGroup(true);
  };
 

  const navigate = useNavigate();
  const dispatch = useDispatch();

  const columns = [
    {
      title: "Message",
      dataIndex: "tmpMessage",
    },
    {
      title: "Created date",
      render: (item) => {
        return <div>{dateForHumans(item)}</div>;
      },
      dataIndex: "tmpCreatedOn",
    },
    {
      title: "Reseller",
      dataIndex: "tmpResellerName",
    },
    {
      title: "Actions",
      render: (item) => (
        <>
          <button onClick={()=>setProdd(item)}>
            <div className="text-darkGreen" onClick={()=>showModalEdit()}>Edit</div> 
          </button>
        </>
      ),
    },
  ];

  const [selectedRowKeys, setSelectedRowKeys] = useState([]);

  const [rowId, setRowId] = useState([]);



  const handleEmployeeToReturns = async (selectedRows) => {
    await setRowId(selectedRows);
    await showModalGroup();
  };

  const onSelectChange = (keys, rows) => {
    setSelectedRowKeys(keys);
    handleEmployeeToReturns(rows);
  };

  const rowSelection = {
    selectedRowKeys,
    onChange: onSelectChange,
    type: "radio",
  };
  // folderObj?.groupId

  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  async function fetchTemplatesData(page, size) {
    await dispatch(
      fetchTemplates({
        url: "api/v2/msgTemp/list",
        limit: size ?? pageSize,
        start: page ?? pageIndex,
      })
    );
  }

  useEffect(() => {
    fetchTemplatesData();
  }, []);

  return (
    <>
      <div className="w-full overflow-y-scroll h-full">
        <InsideHeader
          title="Sms Templates"
          subtitle="Manage your Sms Templates here"
          back={true}
        />

        <div className="lg:px-10 px-3">
          {loadingSms ? (
            <Skeleton />
          ) : (
            <div>
              {templatesData && templatesData?.length > 0 ? (
                <>
                  <div className="flex flex-col">
                    <div className="mt-[1.31rem] flex justify-between items-center gap-x-10">
                      <div className="flex justify-start gap-x-10">
                        <div className={`w-[250px] `}>
                          <button
                            className={`cstm-btn   !rounded-[4px] !bg-[#A3A2A7] !text-[.75rem] flex items-center gap-x-3`}
                            onClick={showModal}
                          > 
                              <MaterialIcon color="#fff" icon="article" /> 
                            Create new template
                          </button>
                        </div>

                        {/* <div className={`w-[250px]`}>
                          <button
                            className={`cstm-btn  !rounded-[4px] !bg-[#A3A2A7] !text-[.75rem] flex items-center gap-x-3`}
                            onClick={showModalGroup}
                          >
                            <img src={svg32} alt="svg32" />
                            Send Sms
                          </button>
                        </div> */}
                      </div>
                    </div>
                    <div className="ml-[20%]"></div>
                  </div>
                  <Table
                    rowSelection={rowSelection}
                    className="mt-[1.31rem] w-full mb-10"
                    scroll={{
                      x: 800,
                    }}
                    pagination={{
                      position: ["bottomCenter"],
                      current: pageIndex + 1,
                      total: templateCount,
                      pageSize: pageSize,
                      onChange: (page, size) => {
                        setPageIndex(page - 1);
                        setPageSize(size);
                        fetchTemplatesData(page - 1, size);
                      },
                      showSizeChanger: false,
                      hideOnSinglePage: true,
                    }}
                    rowKey={(record) => record?.tmpId}
                    columns={columns}
                    dataSource={templatesData}
                    loading={loading}
                  />
                </>
              ) : (
                <div className="mt-[3.19rem] ">
                  <h3 className="font-[700] text-[24px] text-black1 dash-title ">
                    Let’s start by creating your templates
                  </h3>

                  <div
                    className="mt-[31px] bg-white full h-[471.33px] rounded-[15px] border border-solid
               border-[#F5F7FF] py-[60px] flex flex-col justify-center items-center cursor-pointer"
                    onClick={showModal}
                  >
                    <div>
                      <img
                        src={noCon}
                        alt="noCon"
                        className="object-contain h-[291.33px] w-[292px]"
                      />
                      <div className="empty-desc mt-0 flex items-center justify-center">
                        Create your custom template to reuse
                      </div>
                    </div>

                    <div className="mt-[1.63rem] w-[270px">
                      <button className="cstm-btn">
                        <img src={svg25} alt="svg25" />
                        Add template
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      <TemplateModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        prodd={prodd}
        title={`Create new template`}
      />

      <SmsGroupModal
        isModalOpen={isModalOpenGroup}
        setIsModalOpen={setIsModalOpenGroup}
        rowId={rowId}
      />
    </>
  );
}

export default SmsTemplateList;
