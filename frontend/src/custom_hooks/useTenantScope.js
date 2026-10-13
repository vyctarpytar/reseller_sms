import { useSyncExternalStore } from "react";

// Tenant scope = which reseller / account the TOP or RESELLER user is currently
// "acting as". The axios interceptor stamps every request with it (reseller_id /
// account_id), and <Root> picks the sidebar menu from it.
//
// It is kept PER TAB, in sessionStorage. It used to live in localStorage, which
// every tab shares and which several unrelated paths wipe wholesale
// (`localStorage.clear()` on the login page, on logout, on a 401/403): opening the
// login page in a second tab, or one rejected request, silently dropped the
// reseller an operator was working in. sessionStorage survives a reload of the tab
// and belongs to nobody else.
//
// This module is the only reader and writer of those keys. Everything goes through
// one in-memory snapshot, so a consumer can never hold a different value from the
// one the interceptor is sending — a stale copy in <Root> was why re-entering the
// same reseller after a re-login left the TOP sidebar in place.

const KEYS = {
  org: "selectedOrg",
  orgName: "selectedOrgName",
  account: "selectedAccount",
  accountName: "selectedAccountName",
};

function read() {
  return {
    selectedOrg: sessionStorage.getItem(KEYS.org),
    selectedOrgName: sessionStorage.getItem(KEYS.orgName),
    selectedAccount: sessionStorage.getItem(KEYS.account),
    selectedAccountName: sessionStorage.getItem(KEYS.accountName),
  };
}

let snapshot = read();
const listeners = new Set();

function subscribe(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function commit() {
  const next = read();
  const changed = Object.keys(next).some((k) => next[k] !== snapshot[k]);
  if (!changed) return;
  snapshot = next;
  listeners.forEach((listener) => listener());
}

function write(key, value) {
  if (value === undefined) return; // leave untouched
  if (value === null || value === "") {
    sessionStorage.removeItem(key);
  } else {
    sessionStorage.setItem(key, value);
  }
}

// Non-reactive read of the current scope, for code outside React (the axios
// interceptor). Components should use the hook below.
export function getTenantScope() {
  return snapshot;
}

// Update any subset of the scope. Pass `null` to clear a field, omit it to keep.
// e.g. setTenantScope({ org: rsId, orgName: name, account: null, accountName: null })
// A name always belongs to the id beside it: naming a reseller / account without
// its name drops the previous one's name rather than keeping it on the wrong id.
export function setTenantScope({ org, orgName, account, accountName } = {}) {
  write(KEYS.org, org);
  write(KEYS.orgName, org !== undefined && orgName === undefined ? null : orgName);
  write(KEYS.account, account);
  write(
    KEYS.accountName,
    account !== undefined && accountName === undefined ? null : accountName
  );
  commit();
}

export function clearTenantScope() {
  Object.values(KEYS).forEach((key) => sessionStorage.removeItem(key));
  commit();
}

// Reactive read of the current tenant scope. Re-renders the consumer whenever the
// scope changes in this tab.
export function useTenantScope() {
  return useSyncExternalStore(subscribe, getTenantScope);
}

// The layer the user is currently ACTING as, which is what a screen should branch
// on — not `user.layer`, the layer they logged in with. A TOP user who has drilled
// into a reseller (or straight into an account) is doing the reseller's job there:
// they manage that reseller's accounts, not the list of resellers.
export function useActingLayer(userLayer) {
  const { selectedOrg, selectedAccount } = useTenantScope();
  if (userLayer === "TOP" && (selectedOrg || selectedAccount)) return "RESELLER";
  return userLayer;
}
