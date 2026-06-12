import { createSlice, createAsyncThunk } from "@reduxjs/toolkit"; 
import axios from "axios"; 
import axiosInstance from "../../instance";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
	loading: false,
	creditData: [], 
	creditCount:0,
};

export const fetchCreditAccount = createAsyncThunk(
	"creditSlice/fetchCreditAccount",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/credit/account`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );

  export const fetchCreditAdmin = createAsyncThunk(
	"creditSlice/fetchCreditAdmin",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/credit/reseller`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );
 
//   export const fetchCreditReseller = createAsyncThunk(
// 	"creditSlice/fetchCreditReseller",
// 	async (data) => { 
// 		const baseURL = `${url}/api/v2/credit/reseller-account`;
// 		const queryParam = data?.accId ? `?accId=${data.accId}` : '';
// 		const fullURL = `${baseURL}${queryParam}`;
// 	  const res = await axiosInstance
// 		.get(fullURL)
// 		.then((res) => res.data?.data?.result); 
// 	  return res;
// 	}
//   );

  export const fetchCreditReseller = createAsyncThunk('saveSlice/fetch/fetchCreditReseller', async (data, { rejectWithValue }) => {
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

 

export const creditSlice = createSlice({
	name: 'credit',
	initialState,
	reducers: {
		 
	},
	extraReducers: (builder) => {
		builder

		.addCase(fetchCreditAccount.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchCreditAccount.fulfilled, (state, action) => {
			state.loading = false;
			state.creditData = action.payload; 
		  })
		  .addCase(fetchCreditAccount.rejected, (state) => {
			state.loading = false;
			state.creditData = []; 
		  })

		  .addCase(fetchCreditAdmin.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchCreditAdmin.fulfilled, (state, action) => {
			state.loading = false;
			state.creditData = action.payload; 
		  })
		  .addCase(fetchCreditAdmin.rejected, (state) => {
			state.loading = false;
			state.creditData = []; 
		  })

		  .addCase(fetchCreditReseller.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchCreditReseller.fulfilled, (state, action) => {
			state.loading = false;
			state.creditData = action.payload?.data?.result;
			state.creditCount = action.payload.total 
		  })
		  .addCase(fetchCreditReseller.rejected, (state) => {
			state.loading = false;
			state.creditData = []; 
			state.creditCount = 0
		  })
	}
	
	
});

export default creditSlice.reducer;
export const {  } = creditSlice.actions;
