import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import axios from "axios";
import axiosInstance from "../../instance";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
  current: 0,
  requestType: "",
  individualAccData: [],
  loading: false,
  sentSmsData:[],
  customPlaceholder:[],
};

export const fetchMembers = createAsyncThunk(
  "reseller/fetchMembers",
  async () => {
    const res = await axiosInstance
      .get(`${url}/api/v2/member/per-account`)
      .then((res) => res.data?.data?.result);
    return res;
  }
);

export const fetchSentSms = createAsyncThunk(
  "reseller/fetchSentSms",
  async () => {
    const res = await axiosInstance
      .get(`${url}/api/v2/sms`)
      .then((res) => res.data?.data?.result);
    return res;
  }
);

export const fetchPlaceholders = createAsyncThunk(
  "reseller/fetchPlaceholders",
  async () => {
    const res = await axiosInstance
      .get(`${url}/api/v2/sms/placeholders`)
      .then((res) => res.data?.data?.result);
    return res;
  }
);


export const fetchSenderId = createAsyncThunk(
  "reseller/fetchSenderId",
  async () => {
    const res = await axiosInstance
      .get(`${url}/api/v2/sms`)
      .then((res) => res.data?.data?.result);
    return res;
  }
);


export const smsRequestSlice = createSlice({
  name: "sms-request",
  initialState,
  reducers: {
    setCurrent: (state, action) => {
      state.current = action.payload;
    },
    setRequestType: (state, action) => {
      state.requestType = action.payload;
    },
    cleanCurrent: (state, action) => {
      state.current = initialState.current;
    },
    cleanRequestType: (state, action) => {
      state.requestType = initialState.requestType;
    },
  },

  extraReducers: (builder) => {
    builder

      .addCase(fetchMembers.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchMembers.fulfilled, (state, action) => {
        state.loading = false;
        state.individualAccData = action.payload;
      })
      .addCase(fetchMembers.rejected, (state) => {
        state.loading = false;
        state.individualAccData = [];
      })

      .addCase(fetchSentSms.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchSentSms.fulfilled, (state, action) => {
        state.loading = false;
        state.sentSmsData = action.payload;
      })
      .addCase(fetchSentSms.rejected, (state) => {
        state.loading = false;
        state.sentSmsData = [];
      })

      .addCase(fetchPlaceholders.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchPlaceholders.fulfilled, (state, action) => {
        state.loading = false;
        state.customPlaceholder = action.payload;
      })
      .addCase(fetchPlaceholders.rejected, (state) => {
        state.loading = false;
        state.customPlaceholder = [];
      })

     
  },
});

export default smsRequestSlice.reducer;
export const { setCurrent, cleanCurrent, setRequestType, cleanRequestType } =
  smsRequestSlice.actions;
