import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { Spin } from "antd";
import {
  fetchAccountBalance,
  fetchResellerBalance,
  fetchTopBalance,
} from "../../features/menu/menuSlice";
import { useTenantScope } from "../../custom_hooks/useTenantScope";

function DashboardMain() {
  const { user } = useSelector((state) => state.auth);
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { selectedOrg: orgId, selectedAccount: accId } = useTenantScope();

  const fetchBalanceData = () => {
    if (user?.layer === "ACCOUNT" || accId) {
      dispatch(fetchAccountBalance());
    } else if (user?.layer === "RESELLER" || orgId) {
      dispatch(fetchResellerBalance());
    } else if (user?.layer === "TOP") {
      dispatch(fetchTopBalance());
    }
  };

  useEffect(() => {
    fetchBalanceData();
  }, [user, orgId, accId, dispatch]);

  useEffect(() => {
    if (user?.layer === "ACCOUNT" || accId) {
      navigate("/dashboard-account");
    } else if (user?.layer === "RESELLER" || orgId) {
      navigate("/dashboard-reseller");
    } else if (user?.layer === "TOP") {
      navigate("/dashboard");
    }
  }, [user, orgId, accId, navigate]);

  return (
    <div className="w-full h-full min-h-[60vh] flex flex-col items-center justify-center gap-3 bg-surface">
      <Spin size="large" />
      <p className="text-sm text-muted">Loading your dashboard…</p>
    </div>
  );
}

export default DashboardMain;
