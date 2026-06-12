import { createSlice, createAsyncThunk } from "@reduxjs/toolkit"; 
import axios from "axios"; 
import axiosInstance from "../../instance";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
	loading: false,
	loadingStatus: false,
	invoiceData: [], 
	invoiceCount: 0,
	invoDistinctStatus:[]
};

export const fetchInvoices = createAsyncThunk('saveSlice/fetch/saved/sms', async (data, { rejectWithValue }) => {
	let saveUrl = data.url;
	delete data.url;
	try {
		const response = await axiosInstance.post(`${url}/${saveUrl}`, data)
		if (!response.data.success) {
			return rejectWithValue(response.data);
		}   
		return response.data;
	} catch (error) { 
		return rejectWithValue(error.response.data);
	}
  });

  export const fetchinvoDistinctStatus = createAsyncThunk(
	"resellerSlice/fetchinvoDistinctStatus",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/invoice/distinct-statuses`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );



export const invoiceSlice = createSlice({
	name: 'invoice',
	initialState,
	reducers: {
		 
	},
	extraReducers: (builder) => {
		builder

		.addCase(fetchInvoices.pending, (state) => {
			state.loading = true;
		})
		.addCase(fetchInvoices.fulfilled, (state, action) => {
			state.loading = false;
			state.invoiceData= action.payload?.data?.result;
			state.invoiceCount =  action.payload?.total
		})
		.addCase(fetchInvoices.rejected, (state) => {
			state.loading = false;
			state.invoiceData = [];
			state.invoiceCount = 0
		})

		.addCase(fetchinvoDistinctStatus.pending, (state) => {
			state.loadingStatus = true;
		})
		.addCase(fetchinvoDistinctStatus.fulfilled, (state, action) => {
			state.loadingStatus = false; 
			state.invoDistinctStatus =  action.payload
		})
		.addCase(fetchinvoDistinctStatus.rejected, (state) => {
			state.loadingStatus = false; 
			state.invoDistinctStatus = []
		})
	}
	
	
});

export default invoiceSlice.reducer;
export const {  } = invoiceSlice.actions;
