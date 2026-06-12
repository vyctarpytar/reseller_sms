import { createSlice, createAsyncThunk } from "@reduxjs/toolkit"; 
import axios from "axios"; 
import axiosInstance from "../../instance";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
	loading: false,
	resellerAccountData: [], 
	topResellerAccountData: [], 
	singleAcc:{},
};

 
  export const fetchResellerAccounts = createAsyncThunk(
	"reseller/fetchResellerAccounts",
	async () => {
	  const res = await axiosInstance
	  .get(`${url}/api/v2/account`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );
 export const fetchTopResellerAccounts = createAsyncThunk(
	"reseller/fetchTopResellerAccounts",
	async (data) => {
	  const res = await axiosInstance
	  .get(`${url}/api/v2/account/reseller/${data?.resellerId}`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );

  
  export const fetchSingleAccount = createAsyncThunk(
	"account/fetchSingleAccount",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/account/${data?.accId}`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );

  export const fetchAccount = createAsyncThunk('saveSlice/fetch/account', async (data, { rejectWithValue }) => {
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


export const resellerAccountSlice = createSlice({
	name: 'reseller-account',
	initialState,
	reducers: {
		 
	},
	extraReducers: (builder) => {
		builder

		.addCase(fetchResellerAccounts.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchResellerAccounts.fulfilled, (state, action) => {
			state.loading = false;
			state.resellerAccountData = action.payload;
		  })
		  .addCase(fetchResellerAccounts.rejected, (state) => {
			state.loading = false;
			state.resellerAccountData = [];
		  })

		  .addCase(fetchTopResellerAccounts.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchTopResellerAccounts.fulfilled, (state, action) => {
			state.loading = false;
			state.topResellerAccountData = action.payload;
		  })
		  .addCase(fetchTopResellerAccounts.rejected, (state) => {
			state.loading = false;
			state.topResellerAccountData = [];
		  })


		  .addCase(fetchSingleAccount.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchSingleAccount.fulfilled, (state, action) => {
			state.loading = false;
			state.singleAcc = action.payload?.[0];
		  })
		  .addCase(fetchSingleAccount.rejected, (state) => {
			state.loading = false;
			state.singleAcc = {};
		  })



		.addCase(fetchAccount.pending, (state) => {
			state.loading = true;
		})
		.addCase(fetchAccount.fulfilled, (state, action) => {
			state.loading = false;
			state.resellerAccountData= action.payload?.data?.result; 
		})
		.addCase(fetchAccount.rejected, (state) => {
			state.loading = false;
			state.resellerAccountData = []; 
		})

		  
	}
	
	
});

export default resellerAccountSlice.reducer;
export const {  } = resellerAccountSlice.actions;
