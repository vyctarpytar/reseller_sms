import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import axios from "axios";
import axiosInstance from "../../instance";
import qs from "qs";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
  glbLoading: false,
  sideMenuCollapsed: false,
  resellerId: "",
  legendClickStatus: "",
  data: {},
  count: {},
  loading: false,
};

export const downloadFiles = createAsyncThunk(
  "graduate/downloadFiles",
  async (data, { rejectWithValue }) => {
    try {
      const response = await axiosInstance.get(
        `${url}/api/v2/req/download?fileName=${data?.fileName}`,
        {
          responseType: "blob",
        }
      );
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response.data);
    }
  }
);

export const fetchData = createAsyncThunk(
  "fetch/fetchData",
  async (data, { rejectWithValue }) => {
    const { url, ...body } = data;

    await new Promise((resolve) => setTimeout(resolve, 0));

    try {
      const response = await axiosInstance({
        url,
        method: "POST",
        data: Object.keys(body).length ? body : null,
      });
      if (!response.data.success) {
        return {
          key: url,
          data: [],
          total: 0,
        };
      } 
      return {
        key: url,
        data: response?.data?.data?.result,
        total: response?.data?.total,
      };
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);


export const globalSlice = createSlice({
  name: "global",
  initialState,
  reducers: {
    handleSideMenuCollapse: (state, action) => {
      state.sideMenuCollapsed = action.payload;
    },
    setResellerId: (state, action) => {
      state.resellerId = action.payload;
    },
    setLegendClickStatus: (state, action) => {
      state.legendClickStatus = action.payload;
    },

    cleanResellerId: (state, action) => {
      state.resellerId = initialState.resellerId;
    },
    cleanLegendClickStatus: (state, action) => {
      state.legendClickStatus = initialState.legendClickStatus;
    },
  },

  extraReducers: (builder) => {
    builder

      .addCase(downloadFiles.pending, (state) => {
        state.loading = true;
      })
      .addCase(downloadFiles.fulfilled, (state, action) => {
        state.loading = false;
      })
      .addCase(downloadFiles.rejected, (state) => {
        state.loading = false;
      })

      .addCase(fetchData.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchData.fulfilled, (state, action) => {
        state.loading = false;
        const { key, data, total } = action.payload;

        if (!state.data) state.data = {};
        if (!state.count) state.count = {};

        state.data = { ...state.data, [key]: data || [] };
        state.count = { ...state.count, [key]: total || 0 };
      })
      .addCase(fetchData.rejected, (state, action) => {
        state.loading = false;
        const key = action.payload?.key || "unknown";

        if (!state.data) state.data = {};
        if (!state.count) state.count = {};

        state.data = { ...state.data, [key]: [] };
        state.count = { ...state.count, [key]: 0 };
      });
  },
});

export default globalSlice.reducer;
export const {
  handleSideMenuCollapse,
  setResellerId,
  cleanResellerId,
  setLegendClickStatus,
  cleanLegendClickStatus,
} = globalSlice.actions;
