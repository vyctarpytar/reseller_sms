import React, { useState } from "react";
import { Dropdown, Input } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { fetchReseller } from "../features/reseller/resellerSlice";
import { fetchTopResellerAccounts } from "../features/reseller-account/resellerAccountSlice";
import {
  useTenantScope,
  setTenantScope,
  clearTenantScope,
} from "../custom_hooks/useTenantScope";

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
const CheckIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="shrink-0 text-accent">
    <path d="m5 12 5 5 9-10" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const HeaderCrumb = () => {
  const navigate = useNavigate();
  // Reactive tenant scope — updates live no matter which page changes it.
  const { selectedOrg, selectedAccount, selectedAccountName: scopeAccountName } =
    useTenantScope();

  const { resellerData } = useSelector((state) => state.reseller);
  const { topResellerAccountData } = useSelector(
    (state) => state.resellerAccount
  );
  const [searchValue, setSearchValue] = useState("");
  const [orgOpen, setOrgOpen] = useState(false);
  const [accOpen, setAccOpen] = useState(false);
  const dispatch = useDispatch();

  const handleOrgClick = (item) => {
    setTenantScope({ org: item?.rsId, account: null, accountName: null });
    setOrgOpen(false);
    navigate("/dashboard-main");
  };

  const handleAccClick = (item) => {
    setTenantScope({ account: item?.accId, accountName: item?.accName });
    setAccOpen(false);
    navigate("/dashboard-main");
  };

  const handleRemove = () => {
    clearTenantScope();
    navigate("/dashboard");
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
    return found?.accName ?? scopeAccountName ?? null;
  }, [selectedAccount, topResellerAccountData, scopeAccountName]);

  // Shared list panel used by both the reseller and account switchers (desktop).
  const listPanel = (placeholder, items, render, empty) => (
    <div className="w-72 rounded-card border border-border bg-white shadow-lift p-2">
      <Input
        autoFocus
        size="middle"
        allowClear
        placeholder={placeholder}
        value={searchValue}
        onChange={(e) => setSearchValue(e.target.value)}
        className="mb-2"
      />
      {/* shrink-0 on each row is essential: without it the flex column compresses
          rows vertically to fit max-h, clipping the text (the "congested" look). */}
      <div className="max-h-72 overflow-y-auto flex flex-col gap-1 pr-0.5">
        {items?.length > 0 ? items.map(render) : (
          <div className="px-3 py-8 text-center text-xs text-muted">{empty}</div>
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
          className={`shrink-0 block w-full text-left rounded-md px-3 py-2 text-sm leading-5 truncate transition-colors ${
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
          className={`shrink-0 block w-full text-left rounded-md px-3 py-2 text-sm leading-5 truncate transition-colors ${
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

  // ───────────────────────── Mobile scope switcher ─────────────────────────
  // On phones the 3-part inline breadcrumb (root › reseller › account) collides
  // with the logo + avatar. Below `lg` we collapse it into ONE compact button
  // that opens a single sheet: pick reseller, then account. Desktop is unchanged.
  const [mobileOpen, setMobileOpen] = useState(false);
  const [mResellerSearch, setMResellerSearch] = useState("");
  const [mAccountSearch, setMAccountSearch] = useState("");

  const mFilteredResellers = resellerData?.filter((item) =>
    item?.rsCompanyName?.toLowerCase()?.includes(mResellerSearch?.toLowerCase())
  );
  const mFilteredAccounts = topResellerAccountData?.filter((item) =>
    item?.accName?.toLowerCase()?.includes(mAccountSearch?.toLowerCase())
  );

  const mobileLabel = selectedAccountName || selectedOrgName || "All resellers";

  // Pick a reseller: set scope, load its accounts, and KEEP the sheet open so the
  // user can immediately choose an account in the section that appears below.
  const mPickReseller = (item) => {
    setTenantScope({ org: item?.rsId, account: null, accountName: null });
    setMAccountSearch("");
    dispatch(fetchTopResellerAccounts({ resellerId: item?.rsId }));
    navigate("/dashboard-main");
  };
  const mPickAccount = (item) => {
    setTenantScope({ account: item?.accId, accountName: item?.accName });
    setMobileOpen(false);
    navigate("/dashboard-main");
  };
  const mReset = () => {
    setMobileOpen(false);
    handleRemove();
  };

  const mobilePanel = (
    // stopPropagation so taps inside the sheet don't bubble up and toggle the dropdown.
    <div
      onClick={(e) => e.stopPropagation()}
      className="w-[86vw] max-w-[360px] rounded-card border border-border bg-white shadow-lift p-3"
    >
      {/* Reset → platform overview */}
      <button
        type="button"
        onClick={mReset}
        className={`flex items-center gap-2 w-full rounded-md px-3 py-2 text-sm font-medium transition-colors ${
          !selectedOrg ? "bg-accent/10 text-accent" : "text-ink hover:bg-surface"
        }`}
      >
        <GridIcon />
        <span>Platform overview</span>
        <span className="ml-auto">{!selectedOrg && <CheckIcon />}</span>
      </button>

      <div className="border-t border-border my-2.5" />

      {/* Reseller picker */}
      <p className="text-[10px] uppercase tracking-wider text-muted mb-1.5 px-1">
        Reseller
      </p>
      <Input
        size="middle"
        allowClear
        placeholder="Search reseller"
        value={mResellerSearch}
        onChange={(e) => setMResellerSearch(e.target.value)}
        className="mb-2"
      />
      <div className="max-h-44 overflow-y-auto flex flex-col gap-1 pr-0.5">
        {mFilteredResellers?.length > 0 ? (
          mFilteredResellers.map((item) => {
            const active = item?.rsId === selectedOrg;
            return (
              <button
                key={item?.rsId}
                onClick={() => mPickReseller(item)}
                className={`shrink-0 flex items-center w-full text-left rounded-md px-3 py-2.5 text-sm leading-5 transition-colors ${
                  active ? "bg-accent/10 text-accent font-medium" : "text-ink hover:bg-surface"
                }`}
              >
                <span className="truncate">{item?.rsCompanyName}</span>
                {active && <span className="ml-auto pl-2"><CheckIcon /></span>}
              </button>
            );
          })
        ) : (
          <div className="px-3 py-6 text-center text-xs text-muted">No resellers found</div>
        )}
      </div>

      {/* Account picker — appears only once a reseller is chosen */}
      {selectedOrg && (
        <>
          <p className="text-[10px] uppercase tracking-wider text-muted mb-1.5 mt-3.5 px-1">
            Account
          </p>
          <Input
            size="middle"
            allowClear
            placeholder="Search account"
            value={mAccountSearch}
            onChange={(e) => setMAccountSearch(e.target.value)}
            className="mb-2"
          />
          <div className="max-h-44 overflow-y-auto flex flex-col gap-1 pr-0.5">
            {mFilteredAccounts?.length > 0 ? (
              mFilteredAccounts.map((item) => {
                const active = item?.accId === selectedAccount;
                return (
                  <button
                    key={item?.accId}
                    onClick={() => mPickAccount(item)}
                    className={`shrink-0 flex items-center w-full text-left rounded-md px-3 py-2.5 text-sm leading-5 transition-colors ${
                      active ? "bg-accent/10 text-accent font-medium" : "text-ink hover:bg-surface"
                    }`}
                  >
                    <span className="truncate">{item?.accName}</span>
                    {active && <span className="ml-auto pl-2"><CheckIcon /></span>}
                  </button>
                );
              })
            ) : (
              <div className="px-3 py-6 text-center text-xs text-muted">No accounts found</div>
            )}
          </div>
        </>
      )}
    </div>
  );

  return (
    <>
      {/* ── Desktop: inline breadcrumb (unchanged) ── */}
      <nav className="hidden lg:flex items-center gap-1 text-sm" aria-label="Breadcrumb">
        {/* Root — clears all context, back to the platform view. */}
        <button
          type="button"
          onClick={handleRemove}
          title="Platform overview"
          className="inline-flex items-center gap-1.5 rounded-md px-2 py-1 text-muted font-medium hover:text-accent hover:bg-black/5 transition-colors"
        >
          <GridIcon />
          <span className="hidden sm:inline">SMS INST.</span>
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
            setSearchValue("");
            if (visible) dispatch(fetchReseller());
          }}
        >
          <button
            type="button"
            className={`${pillClass} ${selectedOrgName ? "text-primary" : "text-muted"}`}
          >
            <span className="max-w-[92px] lg:max-w-[180px] truncate">
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
                setSearchValue("");
                if (visible)
                  dispatch(fetchTopResellerAccounts({ resellerId: selectedOrg }));
              }}
            >
              <button
                type="button"
                className={`${pillClass} ${selectedAccountName ? "text-primary" : "text-muted"}`}
              >
                <span className="max-w-[92px] lg:max-w-[180px] truncate">
                  {selectedAccountName ? selectedAccountName : "Select account"}
                </span>
                <CaretDown />
              </button>
            </Dropdown>
          </>
        )}
      </nav>

      {/* ── Mobile: single scope button → combined picker sheet ── */}
      <div className="flex lg:hidden min-w-0">
        <Dropdown
          trigger={["click"]}
          open={mobileOpen}
          placement="bottomLeft"
          dropdownRender={() => mobilePanel}
          onOpenChange={(visible) => {
            setMobileOpen(visible);
            if (visible) {
              setMResellerSearch("");
              setMAccountSearch("");
              dispatch(fetchReseller());
              if (selectedOrg)
                dispatch(fetchTopResellerAccounts({ resellerId: selectedOrg }));
            }
          }}
        >
          <button
            type="button"
            className="inline-flex items-center gap-1.5 min-w-0 max-w-[150px] rounded-md border border-border bg-surface px-2.5 py-1.5 text-[13px] font-semibold text-primary transition-colors hover:border-accent/50 active:border-accent/50"
          >
            <span className="text-muted shrink-0"><GridIcon /></span>
            <span className="truncate">{mobileLabel}</span>
            <span className="shrink-0"><CaretDown /></span>
          </button>
        </Dropdown>
      </div>
    </>
  );
};

export default HeaderCrumb;
