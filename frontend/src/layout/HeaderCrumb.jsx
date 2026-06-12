import React, { useState } from "react";
import { Dropdown, Input } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { fetchReseller } from "../features/reseller/resellerSlice";
import { fetchTopResellerAccounts } from "../features/reseller-account/resellerAccountSlice";

// Small inline icons so the breadcrumb reads cleanly without the old svg assets.
const GridIcon = () => (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" className="shrink-0">
    <path
      d="M4 4h6v6H4V4Zm10 0h6v6h-6V4ZM4 14h6v6H4v-6Zm10 0h6v6h-6v-6Z"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);
const ChevronRight = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="shrink-0 text-muted/50">
    <path d="m9 6 6 6-6 6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);
const CaretDown = () => (
  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" className="shrink-0 opacity-70">
    <path d="m6 9 6 6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const HeaderCrumb = () => {
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
  const [orgOpen, setOrgOpen] = useState(false);
  const [accOpen, setAccOpen] = useState(false);
  const dispatch = useDispatch();

  const handleOrgClick = async (item) => {
    await localStorage.setItem("selectedOrg", item?.rsId);
    await localStorage.removeItem("selectedAccount");
    await localStorage.removeItem("selectedAccountName");
    await setSelectedOrg(item?.rsId);
    await setSelectedAccount(null);
    await setOrgOpen(false);
    await navigate("/dashboard-main");
  };

  const handleAccClick = async (item) => {
    await localStorage.setItem("selectedAccount", item?.accId);
    await localStorage.setItem("selectedAccountName", item?.accName);
    await setSelectedAccount(item?.accId);
    await setAccOpen(false);
    await navigate("/dashboard-main");
  };

  const handleRemove = async () => {
    localStorage.removeItem("selectedAccount");
    localStorage.removeItem("selectedAccountName");
    localStorage.removeItem("selectedOrg");
    setSelectedOrg(null);
    setSelectedAccount(null);
    await navigate("/dashboard");
  };

  const filteredResellers = resellerData?.filter((item) =>
    item?.rsCompanyName?.toLowerCase()?.includes(searchValue?.toLowerCase())
  );
  const filteredAccounts = topResellerAccountData?.filter((item) =>
    item?.accName?.toLowerCase()?.includes(searchValue?.toLowerCase())
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
    return (
      found?.accName ?? localStorage.getItem("selectedAccountName") ?? null
    );
  }, [selectedAccount, topResellerAccountData]);

  // Shared list panel used by both the reseller and account switchers.
  const listPanel = (placeholder, items, render, empty) => (
    <div className="w-64 rounded-card border border-border bg-white shadow-lift p-2">
      <Input
        size="small"
        allowClear
        placeholder={placeholder}
        value={searchValue}
        onChange={(e) => setSearchValue(e.target.value)}
        className="mb-2"
      />
      <div className="max-h-60 overflow-y-auto flex flex-col gap-0.5">
        {items?.length > 0 ? items.map(render) : (
          <div className="px-3 py-6 text-center text-xs text-muted">{empty}</div>
        )}
      </div>
    </div>
  );

  const resellerPanel = listPanel(
    "Search reseller",
    filteredResellers,
    (item) => {
      const active = item?.rsId === selectedOrg;
      return (
        <button
          key={item?.rsId}
          onClick={() => handleOrgClick(item)}
          className={`text-left rounded-md px-3 py-2 text-sm transition-colors truncate ${
            active ? "bg-accent/10 text-accent font-medium" : "text-ink hover:bg-surface"
          }`}
        >
          {item?.rsCompanyName}
        </button>
      );
    },
    "No resellers found"
  );

  const accountPanel = listPanel(
    "Search account",
    filteredAccounts,
    (item) => {
      const active = item?.accId === selectedAccount;
      return (
        <button
          key={item?.accId}
          onClick={() => handleAccClick(item)}
          className={`text-left rounded-md px-3 py-2 text-sm transition-colors truncate ${
            active ? "bg-accent/10 text-accent font-medium" : "text-ink hover:bg-surface"
          }`}
        >
          {item?.accName}
        </button>
      );
    },
    "No accounts found"
  );

  const pillClass =
    "inline-flex items-center gap-1.5 rounded-md border border-border bg-surface px-2.5 py-1 font-semibold transition-colors hover:border-accent/50 hover:text-accent";

  return (
    <nav className="flex items-center gap-1 text-sm" aria-label="Breadcrumb">
      {/* Root — clears all context, back to the platform view. */}
      <button
        type="button"
        onClick={handleRemove}
        title="Platform overview"
        className="inline-flex items-center gap-1.5 rounded-md px-2 py-1 text-muted font-medium hover:text-accent hover:bg-black/5 transition-colors"
      >
        <GridIcon />
        <span>SMS INST.</span>
      </button>

      <ChevronRight />

      {/* Reseller switcher. */}
      <Dropdown
        trigger={["click"]}
        open={orgOpen}
        placement="bottomLeft"
        dropdownRender={() => resellerPanel}
        onOpenChange={(visible) => {
          setOrgOpen(visible);
          if (visible) dispatch(fetchReseller());
        }}
      >
        <button
          type="button"
          className={`${pillClass} ${selectedOrgName ? "text-primary" : "text-muted"}`}
        >
          <span className="max-w-[180px] truncate">
            {selectedOrgName ? selectedOrgName : "Select reseller"}
          </span>
          <CaretDown />
        </button>
      </Dropdown>

      {/* Account switcher — only once a reseller is chosen. */}
      {selectedOrg && (
        <>
          <ChevronRight />
          <Dropdown
            trigger={["click"]}
            open={accOpen}
            placement="bottomLeft"
            dropdownRender={() => accountPanel}
            onOpenChange={(visible) => {
              setAccOpen(visible);
              if (visible)
                dispatch(fetchTopResellerAccounts({ resellerId: selectedOrg }));
            }}
          >
            <button
              type="button"
              className={`${pillClass} ${selectedAccountName ? "text-primary" : "text-muted"}`}
            >
              <span className="max-w-[180px] truncate">
                {selectedAccountName ? selectedAccountName : "Select account"}
              </span>
              <CaretDown />
            </button>
          </Dropdown>
        </>
      )}
    </nav>
  );
};

export default HeaderCrumb;
