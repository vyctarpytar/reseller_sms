import React, { useEffect, useState } from "react";
import withdrawal from "../../assets/svg/withdrawalSvg.svg";
import svg46 from "../../assets/svg/svg46.svg";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { fetchBills, fetchMpesaBalance } from "../../features/billing/billingSlice";
import { Skeleton } from "antd";
import { cashConverter, numberWithCommas } from "../../utils";
import BuyUnitsModal from "./modal/BuyUnitsModal";

function WithdrawalHeader() {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [buyOpen, setBuyOpen] = useState(false);

  const handleWithdraw = () => {
    navigate("/withdrawal-page");
  };
  const { loading, billsData, mpesaBalance, mpesaBalanceLoading } = useSelector(
    (state) => state.billing
  );
  const { user } = useSelector((state) => state.auth);
  const isTop = user?.layer === "TOP";

  async function fetchBillsData() {
    await dispatch(fetchBills());
  }

  useEffect(() => {
    fetchBillsData();
    // TOP sees the live Safaricom paybill float in addition to the internal wallet.
    if (isTop) dispatch(fetchMpesaBalance());
  }, [isTop]);

  const wallet = billsData?.[0] || {};
  const walAmount = Number(wallet?.walAmount) || 0;
  const unitPrice = Number(wallet?.walUnitPrice) || 0;
  const affordableUnits = unitPrice > 0 ? Math.floor(walAmount / unitPrice) : 0;
  const mpesaWorking = mpesaBalance?.workingBalance;
  const mpesaUtility = mpesaBalance?.utilityBalance;

  return (
    <div
      className="relative overflow-hidden rounded-[16px] mt-3 p-6 lg:p-8 text-white"
      style={{
        background:
          "linear-gradient(135deg, #13161D 0%, #1c1812 55%, #2c2014 100%)",
      }}
    >
      {/* soft accent glow, top-right */}
      <div
        className="pointer-events-none absolute -top-24 -right-24 w-64 h-64 rounded-full opacity-[0.18]"
        style={{ background: "radial-gradient(circle, #D96C3B 0%, transparent 70%)" }}
      />

      <div className="text-[12px] font-[600] tracking-[0.16em] uppercase text-saOrange">
        Available Balance
      </div>

      {loading ? (
        <Skeleton
          active
          title={{ width: 200 }}
          paragraph={false}
          className="mt-3"
        />
      ) : (
        <div className="font-lexendS text-white text-[34px] lg:text-[42px] font-[600] leading-[1.1] mt-2">
          {walAmount ? cashConverter(walAmount) : "KES 0"}
        </div>
      )}

      <div className="h-[3px] w-[56px] bg-saOrange rounded-full mt-3" />

      <div className="flex flex-wrap items-end justify-between gap-4 mt-5">
        <div className="flex gap-x-10">
          <div>
            <div className="text-white font-[600] text-[15px]">
              {cashConverter(walAmount)}
            </div>
            <div className="text-[11px] tracking-[0.08em] uppercase text-[#8a8f99] mt-0.5">
              Wallet Balance
            </div>
          </div>
          {!isTop && unitPrice > 0 && (
            <div>
              <div className="text-white font-[600] text-[15px]">
                {numberWithCommas(affordableUnits)} units
              </div>
              <div className="text-[11px] tracking-[0.08em] uppercase text-[#8a8f99] mt-0.5">
                Buyable @ {cashConverter(unitPrice)}
              </div>
            </div>
          )}
          {isTop && (
            <div>
              <div className="text-white font-[600] text-[15px]">
                {mpesaBalanceLoading
                  ? "…"
                  : mpesaWorking != null
                  ? cashConverter(mpesaWorking)
                  : "Unavailable"}
              </div>
              <div className="text-[11px] tracking-[0.08em] uppercase text-[#8a8f99] mt-0.5">
                M-Pesa Working Balance
              </div>
            </div>
          )}
          {isTop && (
            <div>
              <div className="text-white font-[600] text-[15px]">
                {mpesaBalanceLoading
                  ? "…"
                  : mpesaUtility != null
                  ? cashConverter(mpesaUtility)
                  : "Unavailable"}
              </div>
              <div className="text-[11px] tracking-[0.08em] uppercase text-[#8a8f99] mt-0.5">
                M-Pesa Utility Balance
              </div>
            </div>
          )}
        </div>

        <div className="px-2.5 py-1 rounded-[6px] text-[12px] font-[600] text-saOrange bg-[rgba(217,108,59,0.12)]">
          {wallet?.walCurrency || "KES"}
        </div>
      </div>

      <div
        className={`grid grid-cols-1 ${
          isTop ? "" : "sm:grid-cols-2"
        } gap-3 mt-6`}
      >
        {!isTop && (
          <button
            type="button"
            onClick={() => setBuyOpen(true)}
            className="h-[46px] rounded-[10px] flex items-center justify-center gap-x-2 font-[600] text-saOrange transition-colors"
            style={{
              background:
                "linear-gradient(135deg, rgba(217,108,59,0.22) 0%, rgba(217,108,59,0.10) 100%)",
              border: "1px solid rgba(217,108,59,0.45)",
            }}
          >
            + Buy Units from Wallet
          </button>
        )}
        <button
          type="button"
          onClick={handleWithdraw}
          className="h-[46px] rounded-[10px] flex items-center justify-center gap-x-2 font-[600] text-white transition-colors hover:bg-[rgba(255,255,255,0.10)]"
          style={{
            background: "rgba(255,255,255,0.06)",
            border: "1px solid rgba(255,255,255,0.14)",
          }}
        >
          <img src={withdrawal} alt="withdrawal" className="w-4 h-4 opacity-90" />
          Withdraw Earnings
        </button>
      </div>

      <BuyUnitsModal open={buyOpen} onClose={() => setBuyOpen(false)} />

      {!isTop && (
        <div className="relative mt-6 bg-[rgba(255,255,255,0.06)] p-2.5 rounded-[8px] flex items-center gap-x-2 text-[12px] text-[#c9ccd2]">
          <img src={svg46} alt="info" className="opacity-80" />
          Earnings update is expected within 30 to 60 min following the users
          purchase of the product
        </div>
      )}
    </div>
  );
}

export default WithdrawalHeader;
