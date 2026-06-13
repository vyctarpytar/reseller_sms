import React, { useState } from "react";
import { Dropdown, Input } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { fetchResellerAccounts } from "../features/reseller-account/resellerAccountSlice";
import { useTenantScope, setTenantScope } from "../custom_hooks/useTenantScope";

// Small inline icons so the breadcrumb reads cleanly without the old svg assets.
const HomeIcon = () => (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" className="shrink-0">
    <path
      d="M3 10.5 12 4l9 6.5M5 9.5V19a1 1 0 0 0 1 1h3v-5h6v5h3a1 1 0 0 0 1-1V9.5"
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

// Breadcrumb shown to a RESELLER once they have drilled into one of their
// accounts (selectedAccount set): a "back to my portal" crumb that clears the
// account context, plus a dropdown to jump straight to another account.
const ResellerCrumb = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const { resellerAccountData } = useSelector((state) => state.resellerAccount);
  const { balanceHeader } = useSelector((state) => state.menu);

  const { selectedAccount, selectedAccountName } = useTenantScope();
  const [searchValue, setSearchValue] = useState("");
  const [open, setOpen] = useState(false);

  const handleAccClick = (item) => {
    setTenantScope({ account: item?.accId, accountName: item?.accName });
    setOpen(false);
    navigate("/dashboard-main");
  };

  // Exit the account: drop the account context and fall back to the
  // reseller's own dashboard (DashboardMain routes RESELLER -> reseller).
  const handleExit = () => {
    setTenantScope({ account: null, accountName: null });
    navigate("/dashboard-main");
  };

  const filteredAccounts = resellerAccountData?.filter((item) =>
    item?.accName?.toLowerCase()?.includes(searchValue?.toLowerCase())
  );

  const accountPanel = (
    <div className="w-72 rounded-card border border-border bg-white shadow-lift p-2">
      <Input
        autoFocus
        size="middle"
        allowClear
        placeholder="Search account"
        value={searchValue}
        onChange={(e) => setSearchValue(e.target.value)}
        className="mb-2"
      />
      <div className="max-h-72 overflow-y-auto flex flex-col gap-1 pr-0.5">
        {filteredAccounts?.length > 0 ? (
          filteredAccounts.map((item) => {
            const active = item?.accId === selectedAccount;
            return (
              <button
                key={item?.accId}
                onClick={() => handleAccClick(item)}
                className={`shrink-0 block w-full text-left rounded-md px-3 py-2 text-sm leading-5 truncate transition-colors ${
                  active
                    ? "bg-accent/10 text-accent font-medium"
                    : "text-ink hover:bg-surface"
                }`}
              >
                {item?.accName}
              </button>
            );
          })
        ) : (
          <div className="px-3 py-8 text-center text-xs text-muted">
            No accounts found
          </div>
        )}
      </div>
    </div>
  );

  // Only relevant once the reseller has actually drilled into an account.
  if (!selectedAccount) return null;

  return (
    <nav className="flex items-center gap-1 text-sm" aria-label="Breadcrumb">
      {/* Parent crumb — exit back to the reseller's own portal. */}
      <button
        type="button"
        onClick={handleExit}
        title="Back to all accounts"
        className="inline-flex items-center gap-1.5 rounded-md px-2 py-1 text-muted font-medium hover:text-accent hover:bg-black/5 transition-colors"
      >
        <HomeIcon />
        <span className="max-w-[96px] lg:max-w-[160px] truncate">
          {balanceHeader?.accName ? balanceHeader?.accName : "My Portal"}
        </span>
      </button>

      <ChevronRight />

      {/* Current account — pill that opens the account switcher. */}
      <Dropdown
        trigger={["click"]}
        open={open}
        placement="bottomLeft"
        dropdownRender={() => accountPanel}
        onOpenChange={(visible) => {
          setOpen(visible);
          setSearchValue("");
          if (visible) dispatch(fetchResellerAccounts());
        }}
      >
        <button
          type="button"
          className="inline-flex items-center gap-1.5 rounded-md border border-border bg-surface px-2.5 py-1 text-primary font-semibold hover:border-accent/50 hover:text-accent transition-colors"
        >
          <span className="max-w-[96px] lg:max-w-[200px] truncate">
            {selectedAccountName ? selectedAccountName : "Select account"}
          </span>
          <CaretDown />
        </button>
      </Dropdown>
    </nav>
  );
};

export default ResellerCrumb;
