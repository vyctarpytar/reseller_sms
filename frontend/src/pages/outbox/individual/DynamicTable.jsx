import React, { useContext, useEffect, useRef, useState } from "react";
import { Button, Form, Input, Popconfirm, Table } from "antd";
import PhoneInput from "react-phone-input-2";
import InsideHeader from "../../../components/InsideHeader";
import SmsGroupModal from "../group/SmsGroupModal";
import svg32 from "../../../assets/svg/svg32.svg";
import svg44 from "../../../assets/svg/svg44.svg";
import svg45 from "../../../assets/svg/svg45.svg";
import SmsMultipleModal from "./SmsMultipleModal";

const EditableContext = React.createContext(null);
const EditableRow = ({ index, ...props }) => {
  const [form] = Form.useForm();
  return (
    <Form form={form} component={false}>
      <EditableContext.Provider value={form}>
        <tr {...props} />
      </EditableContext.Provider>
    </Form>
  );
};
const EditableCell = ({
  title,
  editable,
  children,
  dataIndex,
  record,
  handleSave,
  ...restProps
}) => {
  const [editing, setEditing] = useState(false);
  const inputRef = useRef(null);
  const form = useContext(EditableContext);
  useEffect(() => {
    if (editing) {
      inputRef.current?.focus();
    }
  }, [editing]);

  const toggleEdit = () => {
    setEditing(!editing);
    form.setFieldsValue({
      [dataIndex]:
        record[dataIndex] === "Click to add contact" ? "" : record[dataIndex],
    });
  };
  const save = async () => {
    try {
      const values = await form.validateFields();
      toggleEdit();
      handleSave({
        ...record,
        ...values,
      });
    } catch (errInfo) { 
    }
  };
  let childNode = children;
  if (editable) {
    childNode = editing ? (
      <Form.Item
        style={{
          margin: 0,
        }}
        name={dataIndex}
        rules={[
          {
            required: true,
            message: `${title} is required.`,
          },
          {
            validator: (_, value) => {
              if (value && value.length === 12) {
                return Promise.resolve();
              }
              return Promise.reject(
                new Error("Phone number must be exactly 10 characters")
              );
            },
          },
        ]}
      >
        <PhoneInput
          country="ke"
          onlyCountries={["ke"]}
          countryCodeEditable={false}
          className="input"
          onPressEnter={save}
          onBlur={save}
          onChange={(e) => form.setFieldsValue({ [dataIndex]: e })}
        />
      </Form.Item>
    ) : (
      <div
        className="editable-cell-value-wrap"
        style={{
          paddingInlineEnd: 24,
        }}
        onClick={toggleEdit}
      >
        {record[dataIndex] === "Click to add contact" ? (
          <span style={{ color: "#aaa" }}>Click to add contact</span>
        ) : (
          children
        )}
      </div>
    );
  }
  return <td {...restProps}>{childNode}</td>;
};
const DynamicTable = () => {
  const [dataSource, setDataSource] = useState([]);
  const [count, setCount] = useState(2);
  const handleDelete = (key) => {
    const newData = dataSource.filter((item) => item.key !== key);
    setDataSource(newData);
  };
  const defaultColumns = [
    {
      title: "Phone Number",
      dataIndex: "phone",
      width: "30%",
      editable: true,
    },
    {
      title: "Operation",
      dataIndex: "operation",
      render: (_, record) =>
        dataSource.length >= 1 ? (
          <Popconfirm
            title="Sure to delete?"
            onConfirm={() => handleDelete(record.key)}
            className="cursor-pointer"
          >
            <img src={svg45} alt="svg45" />
          </Popconfirm>
        ) : null,
    },
  ];
  const handleAdd = () => {
    const newData = {
      key: count,
      phone: `Click to add contact`,
    };
    setDataSource([...dataSource, newData]);
    setCount(count + 1);
  };
  const handleSave = (row) => {
    const newData = [...dataSource];
    const index = newData.findIndex((item) => row.key === item.key);
    const item = newData[index];
    newData.splice(index, 1, {
      ...item,
      ...row,
    });
    setDataSource(newData);
  };
  const components = {
    body: {
      row: EditableRow,
      cell: EditableCell,
    },
  };
  const columns = defaultColumns.map((col) => {
    if (!col.editable) {
      return col;
    }
    return {
      ...col,
      onCell: (record) => ({
        record,
        editable: col.editable,
        dataIndex: col.dataIndex,
        title: col.title,
        handleSave,
      }),
    };
  });

  const [isModalOpenGroup, setIsModalOpenGroup] = useState(false);
  const showModalGroup = () => {
    setIsModalOpenGroup(true);
  };

  return (
    <div className="w-full h-full overflow-y-scroll">
      <InsideHeader
        title="One time message"
        subtitle="Click add button"
        back={true}
      />
      <div className="lg:px-10 px-3 mt-10">
        {dataSource?.length > 0 && (
          <div className={`w-[250px]`}>
            <button
              className={`cstm-btn  !rounded-[4px] !bg-[#A3A2A7] !text-[.75rem] flex items-center gap-x-3`}
              onClick={showModalGroup}
            >
              <img src={svg32} alt="svg32" />
              Send New Message
            </button>
          </div>
        )}

        <Table
          className="mt-5"
          components={components}
          rowClassName={() => "editable-row"}
          bordered
          dataSource={dataSource}
          columns={columns}
          pagination={{
            defaultPageSize: 5,
            hideOnSinglePage: true,
            pageSizeOptions: [10, 20, 50, 100],
          }}
          footer={() => (
            <button
              className="flex items-center gap-x-4 text-darkGreen font-lexendS"
              onClick={handleAdd}
            >
              <img src={svg44} alt="svg44" />
              New
            </button>
          )}
        />
      </div>

      <SmsMultipleModal
        isModalOpen={isModalOpenGroup}
        setIsModalOpen={setIsModalOpenGroup}
        dataSource={dataSource}
      />
    </div>
  );
};
export default DynamicTable;
