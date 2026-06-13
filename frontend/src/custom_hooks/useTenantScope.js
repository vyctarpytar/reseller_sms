import { useEffect, useState } from "react";

// Tenant scope (which reseller / account the TOP or RESELLER user is currently
// "acting as") lives in localStorage so the axios interceptor can stamp every
// request with reseller_id / account_id. localStorage is NOT reactive though:
// writing it from a drill-down page (ResellersList, AccountsList, the breadcrumb
// switchers, ...) does not tell the persistent <HeaderCrumb> / <Root> to re-read
// it, so the breadcrumb went stale and the menu/balance kept the old scope until
// a full page reload. These helpers centralize the writes and fire a same-tab
// event so every subscriber updates immediately.

const SCOPE_EVENT = "tenant-scope-changed";

const KEYS = {
  org: "selectedOrg",
  account: "selectedAccount",
  accountName: "selectedAccountName",
};

function emit() {
  // CustomEvent works in all supported browsers; storage events only fire in
  // OTHER tabs, so we need our own for the tab that made the change.
  window.dispatchEvent(new Event(SCOPE_EVENT));
}

function write(key, value) {
  if (value === undefined) return; // leave untouched
  if (value === null || value === "") {
    localStorage.removeItem(key);
  } else {
    localStorage.setItem(key, value);
  }
}

// Update any subset of the scope. Pass `null` to clear a field, omit it to keep.
// e.g. setTenantScope({ org: rsId, account: null, accountName: null })
export function setTenantScope({ org, account, accountName } = {}) {
  write(KEYS.org, org);
  write(KEYS.account, account);
  write(KEYS.accountName, accountName);
  emit();
}

export function clearTenantScope() {
  localStorage.removeItem(KEYS.org);
  localStorage.removeItem(KEYS.account);
  localStorage.removeItem(KEYS.accountName);
  emit();
}

function readScope() {
  return {
    selectedOrg: localStorage.getItem(KEYS.org),
    selectedAccount: localStorage.getItem(KEYS.account),
    selectedAccountName: localStorage.getItem(KEYS.accountName),
  };
}

// Reactive read of the current tenant scope. Re-renders the consumer whenever
// the scope changes in this tab (custom event) or another tab (storage event).
export function useTenantScope() {
  const [scope, setScope] = useState(readScope);

  useEffect(() => {
    const sync = () => setScope(readScope());
    window.addEventListener(SCOPE_EVENT, sync);
    window.addEventListener("storage", sync);
    // Re-sync once on mount in case the scope changed between render and effect.
    sync();
    return () => {
      window.removeEventListener(SCOPE_EVENT, sync);
      window.removeEventListener("storage", sync);
    };
  }, []);

  return scope;
}
