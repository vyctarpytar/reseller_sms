import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import axios from "axios";
import axiosInstance from "../../instance";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
  menuLoading: false,
  menuData: [],
  balanceHeader: {},
};

export const fetchMenu = createAsyncThunk("menuSlice/fetchMenu", async () => {
  const res = await axiosInstance
    .get(`${url}/api/v1/menu`)
    .then((res) => res.data?.data?.result);
  return res;
});

export const fetchResellerBalance = createAsyncThunk(
  "reseller/fetchResellerBalance",
  async () => {
    const res = await axiosInstance
      .get(`${url}/api/v2/res/balance`)
      .then((res) => res.data?.data?.result);
    return res;
  }
);
export const fetchAccountBalance = createAsyncThunk(
  "reseller/fetchAccountBalance",
  async () => {
    const res = await axiosInstance
      .get(`${url}/api/v2/account/balance`)
      .then((res) => res.data?.data?.result);
    return res;
  }
);

export const fetchTopBalance = createAsyncThunk(
  "reseller/fetchTopBalance",
  async () => {
    const res = await axiosInstance
      .get(`${url}/api/v2/res/top-balance`)
      .then((res) => res.data?.data?.result);
    return res;
  }
);

export const menuSlice = createSlice({
  name: "menu",
  initialState,
  reducers: {
    handleSideMenuCollapse: (state, action) => {
      state.sideMenuCollapsed = action.payload;
    },

    cleanBalanceHeader: (state) => {
      state.balanceHeader = initialState.balanceHeader;
    },
  },
  extraReducers: (builder) => {
    builder

      .addCase(fetchMenu.pending, (state) => {
        state.menuLoading = true;
      })
      .addCase(fetchMenu.fulfilled, (state, action) => {
        state.menuLoading = false;
        state.menuData = action.payload;
      })
      .addCase(fetchMenu.rejected, (state) => {
        state.menuLoading = false;
        state.menuData = [];
      })

      .addCase(fetchResellerBalance.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchResellerBalance.fulfilled, (state, action) => {
        state.loading = false;
        state.balanceHeader = action.payload;
      })
      .addCase(fetchResellerBalance.rejected, (state) => {
        state.loading = false;
        state.balanceHeader = {};
      })

      .addCase(fetchAccountBalance.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchAccountBalance.fulfilled, (state, action) => {
        state.loading = false;
        state.balanceHeader = action.payload;
      })
      .addCase(fetchAccountBalance.rejected, (state) => {
        state.loading = false;
        state.balanceHeader = {};
      })


      .addCase(fetchTopBalance.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchTopBalance.fulfilled, (state, action) => {
        state.loading = false;
        state.balanceHeader = action.payload;
      })
      .addCase(fetchTopBalance.rejected, (state) => {
        state.loading = false;
        state.balanceHeader = {};
      })
      
  },
});

export default menuSlice.reducer;
export const { cleanBalanceHeader } = menuSlice.actions;
