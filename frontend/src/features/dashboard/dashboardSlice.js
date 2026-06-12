import { createSlice, createAsyncThunk } from "@reduxjs/toolkit"; 
import axios from "axios"; 
import axiosInstance from "../../instance";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
	loading: false,
	dashData: [], 
	report:[],
	dailySmsData: [],
	daiySmsCount: 0,	
	statusSmsData: [],
	statusSmsCount: 0,	
};

 
  export const fetchDash = createAsyncThunk('saveSlice/fetchDash', async (data, { rejectWithValue }) => {
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

  export const fetchReport = createAsyncThunk('saveSlice/fetchReport', async (data, { rejectWithValue }) => {
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

  export const fetchDailySmsReport = createAsyncThunk('saveSlice/fetchDailySmsReport', async (data, { rejectWithValue }) => {
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
  export const fetchStatusReport = createAsyncThunk('saveSlice/fetchStatusReport', async (data, { rejectWithValue }) => {
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

 

export const dashboardSlice = createSlice({
	name: 'dashboardSlice',
	initialState,
	reducers: {
		 
	},
	extraReducers: (builder) => {
		builder

		.addCase(fetchDash.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchDash.fulfilled, (state, action) => {
			state.loading = false;
			state.dashData = action.payload?.data?.result; 
		  })
		  .addCase(fetchDash.rejected, (state) => {
			state.loading = false;
			state.dashData = []; 
		  })

		  .addCase(fetchReport.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchReport.fulfilled, (state, action) => {
			state.loading = false;
			state.report = action.payload?.data?.result; 
		 
		  })
		  .addCase(fetchReport.rejected, (state) => {
			state.loading = false;
			state.report = []; 
 
		  })

		  .addCase(fetchDailySmsReport.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchDailySmsReport.fulfilled, (state, action) => {
			state.loading = false;
			state.dailySmsData = action.payload?.data?.result; 
			state.daiySmsCount =  action.payload?.total
		  })
		  .addCase(fetchDailySmsReport.rejected, (state) => {
			state.loading = false;
			state.dailySmsData = []; 
			state.daiySmsCount = 0 
		  })

		  .addCase(fetchStatusReport.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchStatusReport.fulfilled, (state, action) => {
			state.loading = false;
			state.statusSmsData = action.payload?.data?.result; 
			state.statusSmsCount =  action.payload?.total
		  })
		  .addCase(fetchStatusReport.rejected, (state) => {
			state.loading = false;
			state.statusSmsData = []; 
			state.statusSmsCount = 0 
		  })

		   
	}
	
	
});

export default dashboardSlice.reducer;
export const {  } = dashboardSlice.actions;
