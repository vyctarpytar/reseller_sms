import React, { useEffect, useState } from "react";
import { Spin } from "antd";
import toast from "react-hot-toast";
import axiosInstance from "../../instance";
import { useTenantScope } from "../../custom_hooks/useTenantScope";

const url = import.meta.env.VITE_API_BASE_URL;

const fmt = (n) => Number(n || 0).toLocaleString("en-KE");

// Self-service low-balance alert level. The backend picks the target from the
// tenant scope the interceptor sends: an account user edits their own account,
// a reseller their own reseller (or the account they are drilled into).
export default function LowBalanceAlertCard() {
  const { selectedOrg, selectedAccount } = useTenantScope();
  const [data, setData] = useState(null);
  const [level, setLevel] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  function apply(result) {
    setData(result);
    setLevel(result?.level != null ? String(result.level) : "");
  }

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    axiosInstance
      .get(`${url}/api/v2/low-balance-alert`)
      .then((res) => {
        if (!cancelled && res?.data?.success !== false) apply(res?.data?.data?.result);
      })
      .catch(() => {
        if (!cancelled) setData(null);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [selectedOrg, selectedAccount]);

  async function submit(nextLevel) {
    setSaving(true);
    try {
      const res = await axiosInstance.post(`${url}/api/v2/low-balance-alert`, {
        level: nextLevel,
      });
      if (res?.data?.success === false) {
        toast.error(res?.data?.messages?.message ?? "Could not save the alert level");
        return;
      }
      apply(res?.data?.data?.result);
      toast.success(res?.data?.messages?.message ?? "Saved");
    } catch (e) {
      toast.error(e?.response?.data?.messages?.message ?? "Could not save the alert level");
    } finally {
      setSaving(false);
    }
  }

  function onSave(e) {
    e.preventDefault();
    const trimmed = String(level).trim();
    if (!/^\d+$/.test(trimmed)) {
      toast.error("Enter a whole number of units (0 turns alerts off)");
      return;
    }
    submit(Number(trimmed));
  }

  if (loading) {
    return (
      <div className="card w-full flex justify-center py-10">
        <Spin />
      </div>
    );
  }
  if (!data) return null;

  const off = Number(data.level) === 0;
  const isReseller = data.scope === "RESELLER";
  const balanceLabel = isReseller ? "Allocatable units" : "Unit balance";
  const below = !off && Number(data.balance) <= Number(data.level);

  return (
    <div className="card w-full flex flex-col gap-4">
      <div className="flex items-start justify-between">
        <div>
          <h3 className="text-lg font-medium text-primary leading-snug">
            Low balance alerts
          </h3>
          <p className="text-sm text-muted leading-relaxed mt-1">
            {isReseller
              ? "We email and SMS you when your allocatable units fall to this level, so your accounts can keep buying units."
              : "We email and SMS your admin when your SMS units fall to this level, so you can top up before sending stops."}
          </p>
        </div>
        <span
          className={`${
            off ? "badge-neutral" : data.usingDefault ? "badge-open" : "badge-active"
          } ml-3 flex-shrink-0`}
        >
          {off ? "Off" : data.usingDefault ? "Default" : "Custom"}
        </span>
      </div>

      <div className="grid grid-cols-2 gap-4 py-4 border-t border-b border-border">
        <div>
          <p className={`text-2xl font-semibold ${below ? "text-accent" : "text-primary"}`}>
            {fmt(data.balance)}
          </p>
          <p className="text-[10px] uppercase tracking-wider text-muted mt-0.5">
            {balanceLabel}
          </p>
        </div>
        <div>
          <p className="text-2xl font-semibold text-primary">
            {off ? "—" : fmt(data.level)}
          </p>
          <p className="text-[10px] uppercase tracking-wider text-muted mt-0.5">
            Alert level
          </p>
        </div>
      </div>

      <form onSubmit={onSave} className="flex flex-col gap-2">
        <label htmlFor="lowBalanceLevel" className="text-xs text-muted">
          Alert me when units fall to
        </label>
        <div className="flex flex-col sm:flex-row gap-3">
          <input
            id="lowBalanceLevel"
            type="number"
            min={0}
            step={1}
            inputMode="numeric"
            value={level}
            onChange={(e) => setLevel(e.target.value)}
            placeholder={String(data.defaultLevel)}
            className="input sm:flex-1"
            disabled={saving}
          />
          <button type="submit" className="btn-primary" disabled={saving}>
            {saving ? <Spin size="small" /> : "Save"}
          </button>
        </div>
        <div className="flex items-center justify-between text-xs text-muted">
          <span>Set 0 to turn alerts off.</span>
          {!data.usingDefault && (
            <button
              type="button"
              className="text-accent hover:underline disabled:opacity-50"
              onClick={() => submit(null)}
              disabled={saving}
            >
              Use default ({fmt(data.defaultLevel)})
            </button>
          )}
        </div>
      </form>
    </div>
  );
}
