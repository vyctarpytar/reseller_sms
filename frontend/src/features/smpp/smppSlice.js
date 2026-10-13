import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import axiosInstance from "../../instance";

const url = import.meta.env.VITE_API_BASE_URL;

const initialState = {
  loading: false,
  smppConnections: [],
};

// reseller_id / account_id are stamped on by the axios interceptor, so the same
// call returns a reseller's connections, or one account's when drilled in.
export const fetchSmppConnections = createAsyncThunk(
  "smpp/fetchSmppConnections",
  async () => {
    const res = await axiosInstance
      .get(`${url}/api/v2/smpp-connection`)
      .then((res) => res.data?.data?.result);
    return res;
  }
);

export const smppSlice = createSlice({
  name: "smpp",
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchSmppConnections.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchSmppConnections.fulfilled, (state, action) => {
        state.loading = false;
        state.smppConnections = action.payload ?? [];
      })
      .addCase(fetchSmppConnections.rejected, (state) => {
        state.loading = false;
        state.smppConnections = [];
      });
  },
});

export default smppSlice.reducer;
