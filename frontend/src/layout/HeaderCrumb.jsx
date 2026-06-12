import React, { useEffect, useState } from "react";
import svg82 from "../assets/svg/svg82.svg";
import svg83 from "../assets/svg/svg83.svg";
import svg84 from "../assets/svg/svg84.svg";
import { Breadcrumb, Dropdown, Menu, Input } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { fetchReseller } from "../features/reseller/resellerSlice";
import {
  fetchResellerAccounts,
  fetchTopResellerAccounts,
} from "../features/reseller-account/resellerAccountSlice";

const HeaderCrumb = () => {
  const url = "auth/counties/getFiltered";
  const navigate = useNavigate();
  const [selectedOrg, setSelectedOrg] = useState(
    localStorage.getItem("selectedOrg")
  );
  const [selectedAccount, setSelectedAccount] = useState(
    localStorage.getItem("selectedAccount")
  );

  const { resellerData } = useSelector((state) => state.reseller);
  const { topResellerAccountData } = useSelector(
    (state) => state.resellerAccount
  );
  const [searchValue, setSearchValue] = useState("");
  const dispatch = useDispatch();
  const [data, setData] = useState([]);

  const handleOrgClick = async (item) => {
    await localStorage.setItem("selectedOrg", item?.rsId);
    await localStorage.removeItem("selectedAccount");
    await setSelectedOrg(item?.rsId);
    await setSelectedAccount(null);
    await navigate("/dashboard-main");
  };

  const handleAccClick = async (item) => {
    await localStorage.setItem("selectedAccount", item?.accId);
    await setSelectedAccount(item?.accId);
    await navigate("/dashboard-main");
  };

  const filteredResellers = resellerData?.filter((item) =>
    item?.rsCompanyName?.toLowerCase()?.includes(searchValue?.toLowerCase())
  );
  const resellerMenu = (
    <Menu>
      <Menu.Item key="search" disabled>
        <Input
          placeholder="Search reseller"
          value={searchValue}
          onChange={(e) => setSearchValue(e.target.value)}
        />
      </Menu.Item>
      <Menu.Divider />
      <div style={{ maxHeight: "250px", overflowY: "auto" }}>
        {filteredResellers?.length > 0 ? (
          filteredResellers?.map((item) => (
            <Menu.Item key={item?.rsId} onClick={() => handleOrgClick(item)}>
              {item?.rsCompanyName}
            </Menu.Item>
          ))
        ) : (
          <Menu.Item disabled>No results found</Menu.Item>
        )}
      </div>
    </Menu>
  );

  const filteredAccounts = topResellerAccountData?.filter((item) =>
    item?.accName?.toLowerCase()?.includes(searchValue?.toLowerCase())
  );
  const accountMenu = (
    <Menu>
      <Menu.Item key="search" disabled>
        <Input
          placeholder="Search account"
          value={searchValue}
          onChange={(e) => setSearchValue(e.target.value)}
        />
      </Menu.Item>
      <Menu.Divider />
      <div style={{ maxHeight: "250px", overflowY: "auto" }}>
        {filteredAccounts?.length > 0 ? (
          filteredAccounts?.map((item) => (
            <Menu.Item key={item?.accId} onClick={() => handleAccClick(item)}>
              {item?.accName}
            </Menu.Item>
          ))
        ) : (
          <Menu.Item disabled>No results found</Menu.Item>
        )}
      </div>
    </Menu>
  );

  const selectedOrgName = React.useMemo(() => {
    if (!selectedOrg) return null;
    const found = resellerData?.find((r) => r?.rsId === selectedOrg);
    return found?.rsCompanyName ?? null;
  }, [selectedOrg, resellerData]);

  const selectedAccountName = React.useMemo(() => {
    if (!selectedAccount) return null;
    const found = topResellerAccountData?.find(
      (a) => a?.accId === selectedAccount
    );
    return found?.accName ?? null;
  }, [selectedAccount, topResellerAccountData]);

  function fetchResellerData() {
    dispatch(fetchReseller());
  }

  function fetchResellerAccountData() {
    dispatch(
      fetchTopResellerAccounts({
        resellerId: selectedOrg,
      })
    );
  }

  const handleRemove = async () => {
    localStorage.removeItem("selectedAccount");
    localStorage.removeItem("selectedOrg");
    setSelectedOrg(null);
    setSelectedAccount(null);
    await navigate("/dashboard");
  };

  return (
    <div>
      <Breadcrumb separator={<img src={svg83} alt="separator" />}>
        <Breadcrumb.Item>
          <span
            className="flex items-center cursor-pointer"
            onClick={handleRemove}
          >
            <img className="mr-[.5rem]" src={svg82} alt="svg82" />
            SMS INST.
          </span>
        </Breadcrumb.Item>

        <Breadcrumb.Item>
          <Dropdown
            overlay={resellerMenu}
            trigger={["click"]}
            onVisibleChange={(visible) => {
              if (visible) {
                fetchResellerData();
              }
            }}
          >
            <span className="flex items-center cursor-pointer">
              <img className="mr-[.5rem]" src={svg82} alt="svg82" />
              {selectedOrgName ? selectedOrgName : "SELECT"}
              <img src={svg84} alt="icon" className="ml-1" />
            </span>
          </Dropdown>
        </Breadcrumb.Item>

        {selectedOrg && (
          <Breadcrumb.Item>
            <Dropdown
              overlay={accountMenu}
              trigger={["click"]}
              onVisibleChange={(visible) => {
                if (visible) {
                  fetchResellerAccountData();
                }
              }}
            >
              <span className="flex items-center cursor-pointer">
                <img className="mr-[.5rem]" src={svg82} alt="svg82" />
                {selectedAccountName ? selectedAccountName : "SELECT"}
                <img src={svg84} alt="icon" className="ml-1" />
              </span>
            </Dropdown>
          </Breadcrumb.Item>
        )}
      </Breadcrumb>
    </div>
  );
};

export default HeaderCrumb;
