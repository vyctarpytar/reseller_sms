import { Button, Form, Input, Select, Spin, Tabs } from "antd";
import React, { useEffect, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import InsideHeader from "../../components/InsideHeader";
import { instTypeData } from "../../data";
import RequestCodeTable from "./RequestCodeTable";
import {
  fetcActiveProductRequest,
  setRefetch,
  setRefetchKey,
} from "../../features/reseller-codes/resellerCodesSlice";

import ActiveCodeTable from "./ActiveCodeTable";
import NewTable from "./NewTable";
import {
  fetchApprovedProductRequest,
  fetchNewProductRequest,
} from "../../features/product-request/productRequestSlice";

function CodesRequestList() {
  const dispatch = useDispatch();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const { user } = useSelector((state) => state.auth);
  const { completedCount, newCount, inProgressCount } = useSelector(
    (state) => state.productRequest
  );
  const { activeCount, refetchKey } = useSelector(
    (state) => state.resellerCodes
  );

  const [form] = Form.useForm();
  const formRef = useRef(null);
  const navigate = useNavigate();

  const handleAddActivity = async () => {
    navigate("/activity-add");
  };

  const [active, setactive] = useState("1");
  const [search, setsearch] = useState("");
  const [formData, setFormData] = useState("");

  async function refetching(activeKey) {
    await dispatch(setRefetch());
    await dispatch(setRefetchKey(activeKey));
    await setactive(activeKey);
  }

  function handleSelectChange(value, formName) {
    setFormData((prevData) => ({
      ...prevData,
      [formName]: value,
    }));
  }

  const { TabPane } = Tabs;

  const tabConfigurations = [
    {
      label: (
        <span className="flex items-center">
          <span className=" ">New({newCount})</span>
        </span>
      ),
      key: "1",
      children: <NewTable />,
    },

    {
      label: (
        <span className="flex items-center">
          <span className=" ">Unassigned({completedCount})</span>
        </span>
      ),
      key: "2",
      children: <RequestCodeTable />,
    },

    {
      label: (
        <span className="flex items-center">
          <span className="">Assigned({activeCount})</span>
        </span>
      ),
      key: "3",
      children: <ActiveCodeTable />,
    },
  ];

  function handleRequestProduct() {
    navigate("/sms-request-list");
  }

  async function fetchNewProductRequestData() {
    const res = await dispatch(
      fetchNewProductRequest({
        reqStatus: "PENDING",
      })
    );
  }

  async function fetchProcessedProductRequestData() {
    const res = await dispatch(
      fetchApprovedProductRequest({
        reqStatus: "PROCESSED",
      })
    );
  }

  async function fetchActiveRequestData() {
    const res = await dispatch(
      fetcActiveProductRequest({
        reqStatus: "ACTIVE",
      })
    );
  }

  useEffect(() => {
    fetchNewProductRequestData();
    fetchProcessedProductRequestData();
    fetchActiveRequestData();
  }, []);
  useEffect(() => {
    if (refetchKey == 1) {
      fetchNewProductRequestData();
    }
  }, [refetchKey]);

  useEffect(() => {
    if (refetchKey == 2) {
      fetchProcessedProductRequestData();
    }
  }, [refetchKey]);

  useEffect(() => {
    if (refetchKey == 3) {
      fetchActiveRequestData();
    }
  }, [refetchKey]);

  return (
    <div className="w-full">
      <InsideHeader
        title="Assign account to a request"
        subtitle="Manage your accounts and request here"
        back={false}
      />

      <div className="mt-10 flex flex-col lg:px-10 px-3">
        <div className="product_request_title">Account assigning</div>
        <div className="product_sub flex lg:flex-row flex-col items-center mt-[1.5rem] !text-[18px]">
          You can assign account to unassigned request. &nbsp;{" "}
          <span
            className="text-[#388E3C] cursor-pointer "
            onClick={handleRequestProduct}
          >
            Request a product.
          </span>
        </div>

        <div className="flex items-center mt-[1rem] lg:w-[50%] w-full">
          <Input
            value={search}
            onChange={(e) => setsearch(e.target.value)}
            placeholder="Search services"
            className="text-[16px] font-[400] h-[40px]"
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

          <Select
            name="rsBusinessType"
            allowClear
            style={{
              width: "100%",
            }}
            onChange={(value) => {
              handleSelectChange(value, "rsBusinessType");
            }}
            value={formData?.rsBusinessType}
            options={
              instTypeData?.length > 0 &&
              instTypeData?.map((item) => ({
                value: item?.value,
                label: item?.label,
              }))
            }
            showSearch
            optionFilterProp="children"
            filterOption={(input, option) =>
              (option?.label?.toLocaleLowerCase() ?? "").includes(
                input?.toLocaleLowerCase()
              )
            }
            placeholder="All"
            onDropdownVisibleChange={() => {}}
          />
        </div>
      </div>

      <div className="h-[111px] lg:px-10 px-3 mt-[1rem]">
        <Tabs
          items={tabConfigurations}
          defaultActiveKey="1"
          activeKey={active}
          onChange={refetching}
          className="activity-tab activity-tab-wrap activity-bar"
        />
      </div>
    </div>
  );
}

export default CodesRequestList;
