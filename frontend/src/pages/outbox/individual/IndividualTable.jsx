import React, { useEffect, useState } from "react";
import ResponsiveTable, { hideBelow } from "../../../components/ResponsiveTable";
import svg32 from "../../../assets/svg/svg32.svg";
import toast from "react-hot-toast";
import SmsIndividualModal from "./SmsIndividualModal";
import { fetchMembers } from "../../../features/sms-request/smsRequestSlice";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";

function IndividualTable() {
 

  const columns = [
    {
      title: "Full Name",
      dataIndex: "chFullName", 
    },
    {
      title: "Gender",
      dataIndex: "chGenderCode",
      ...hideBelow(),
    },
    {
      title: "Date of Birth",
      dataIndex: "chDob",
      ...hideBelow(),
    },
    {
      title: "National ID",
      dataIndex: "chNationalId",
      ...hideBelow(),
    },
    {
      title: "Telephone Number",
      dataIndex: "chTelephone",
    },
  ];
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const { individualAccData } = useSelector((state) => state.sms);
 
  const [isModalOpen, setIsModalOpen] = useState(false);

  const [prodd, setProdd] = useState();

  const [selectedRowKeys, setSelectedRowKeys] = useState([]);

  const [rowId, setRowId] = useState([]);

  const handleEmployeeToReturns = async (selectedRows) => {
    setRowId(selectedRows);
  };

  const showModal = () => {
    setIsModalOpen(true);
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

  async function fetchMembersData() {
    dispatch(fetchMembers());
  }

  useEffect(() => {
    fetchMembersData();
  }, []);

  return (
    <>
      <div className="mt-[3.31rem] ">
        <div className={`w-[200px]`}>
          <button
            className={`cstm-btn  !rounded-[4px] !bg-[#69472E] !text-[.75rem] flex items-center gap-x-3`}
            onClick={showModal}
          >
            <img src={svg32} alt="svg32" />
            Write Message
          </button>
        </div>
      </div>
      <div>
        <ResponsiveTable
          rowSelection={rowSelection}
          className="mt-[1px] w-full"
          scroll={{
            x: 800,
          }}
          rowKey={(record) => record?.chId}
          columns={columns}
          dataSource={individualAccData}
        />
      </div>

      <SmsIndividualModal
        isModalOpen={isModalOpen}
        setIsModalOpen={setIsModalOpen}
        rowId={rowId}
      />
    </>
  );
}

export default IndividualTable;
