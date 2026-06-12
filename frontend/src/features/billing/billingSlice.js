import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import axios from "axios";
import axiosInstance from "../../instance";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
  loading: false,
  billsData: [],
  saving: false,
  withdrawalData: {},
  tariffData: [],

  payoutHistoryData: [],
  payoutHistoryCount: 0,
  payoutDistinctStatus: [],
  paymentHistoryData: [],
  buying: false,
  buyUnitsResult: {},
};

export const fetchBills = createAsyncThunk(
  "billingSlice/fetchBills",
  async (data) => {
    const res = await axiosInstance
      .get(`${url}/api/v2/wallet/balance`)
      .then((res) => res.data?.data?.result);
    return res;
  }
);

export const fetchTariff = createAsyncThunk(
  "billingSlice/fetchTariff",
  async (data) => {
    const res = await axiosInstance
      .get(`${url}/api/v2/wallet/rates`)
      .then((res) => res.data?.data?.result);
    return res;
  }
);

export const saveWithdrawal = createAsyncThunk(
  "billing/saveWithdrawal",
  async (data, { rejectWithValue }) => {
    let saveUrl = data.url;
    delete data.url;
    try {
      const response = await axiosInstance.post(`${url}/${saveUrl}`, data);
      if (!response.data.success) {
        return rejectWithValue(response.data);
      }
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response.data);
    }
  }
);

export const fetchPayoutHistory = createAsyncThunk(
  "saveSlice/fetch/fetchPayoutHistory",
  async (data, { rejectWithValue }) => {
    let saveUrl = data.url;
    delete data.url;
    try {
      const response = await axiosInstance.post(`${url}/${saveUrl}`, data);
      if (!response.data.success) {
        return rejectWithValue(response.data);
      }
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response.data);
    }
  }
);

export const fetchPayoutDistinctStatus = createAsyncThunk(
  "resellerSlice/fetchPayoutDistinctStatus",
  async (data) => {
    const res = await axiosInstance
      .get(`${url}/api/v2/wallet/distinct-status`)
      .then((res) => res.data?.data?.result);
    return res;
  }
);

export const fetchPaymentHistory = createAsyncThunk(
  "billingSlice/fetchPaymentHistory",
  async (data) => {
    const res = await axiosInstance
      .get(`${url}/api/v2/invoice/reseller-summary`)
      .then((res) => res.data?.data?.result);
    return res;
  }
);

// Reseller buys SMS units from TOP using cash-wallet balance (no STK).
export const buyUnitsFromWallet = createAsyncThunk(
  "billing/buyUnitsFromWallet",
  async (data, { rejectWithValue }) => {
    try {
      const response = await axiosInstance.post(`${url}/api/v2/wallet/buy-units`, data);
      if (!response.data.success) {
        return rejectWithValue(response.data);
      }
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data);
    }
  }
);


export const billingSlice = createSlice({
  name: "billing",
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder

      .addCase(fetchBills.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchBills.fulfilled, (state, action) => {
        state.loading = false;
        state.billsData = action.payload;
      })
      .addCase(fetchBills.rejected, (state) => {
        state.loading = false;
        state.billsData = [];
      })

      .addCase(saveWithdrawal.pending, (state) => {
        state.saving = true;
      })
      .addCase(saveWithdrawal.fulfilled, (state, action) => {
        state.saving = false;
        state.withdrawalData = action?.payload?.data?.result;
      })
      .addCase(saveWithdrawal.rejected, (state) => {
        state.saving = false;
        state.withdrawalData = {};
      })

      .addCase(fetchTariff.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchTariff.fulfilled, (state, action) => {
        state.loading = false;
        state.tariffData = action.payload;
      })
      .addCase(fetchTariff.rejected, (state) => {
        state.loading = false;
        state.tariffData = [];
      })

      .addCase(fetchPayoutHistory.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchPayoutHistory.fulfilled, (state, action) => {
        state.loading = false;
        state.payoutHistoryData = action.payload?.data?.result;
        state.payoutHistoryCount = action.payload?.total;
      })
      .addCase(fetchPayoutHistory.rejected, (state) => {
        state.loading = false;
        state.payoutHistoryData = [];
        state.payoutHistoryCount = 0;
      })

      .addCase(fetchPayoutDistinctStatus.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchPayoutDistinctStatus.fulfilled, (state, action) => {
        state.loading = false;
        state.payoutDistinctStatus = action.payload;
      })
      .addCase(fetchPayoutDistinctStatus.rejected, (state) => {
        state.loading = false;
        state.payoutDistinctStatus = [];
      })


      .addCase(fetchPaymentHistory.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchPaymentHistory.fulfilled, (state, action) => {
        state.loading = false;
        state.paymentHistoryData = action.payload;
      })
      .addCase(fetchPaymentHistory.rejected, (state) => {
        state.loading = false;
        state.paymentHistoryData = [];
      })

      .addCase(buyUnitsFromWallet.pending, (state) => {
        state.buying = true;
      })
      .addCase(buyUnitsFromWallet.fulfilled, (state, action) => {
        state.buying = false;
        state.buyUnitsResult = action?.payload?.data?.result;
      })
      .addCase(buyUnitsFromWallet.rejected, (state) => {
        state.buying = false;
        state.buyUnitsResult = {};
      })
  },
});

export default billingSlice.reducer;
export const {} = billingSlice.actions;
