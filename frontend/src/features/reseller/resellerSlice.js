import { createSlice, createAsyncThunk } from "@reduxjs/toolkit"; 
import axios from "axios"; 
import axiosInstance from "../../instance";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
	loading: false,
	resellerData: [], 
	resellerImage:''
};

export const fetchReseller = createAsyncThunk(
	"resellerSlice/fetchReseller",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/res`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );

  export const fetchResellerImage = createAsyncThunk(
	"resellerSlice/fetchResellerImage",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/public/view-logo/${data?.rsId}`)
		.then((res) => res.data); 
	  return res;
	}
  );

 

export const resellerSlice = createSlice({
	name: 'reseller',
	initialState,
	reducers: {
		 
	},
	extraReducers: (builder) => {
		builder

		.addCase(fetchReseller.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchReseller.fulfilled, (state, action) => {
			state.loading = false;
			state.resellerData = action.payload; 
		  })
		  .addCase(fetchReseller.rejected, (state) => {
			state.loading = false;
			state.resellerData = []; 
		  })

		  .addCase(fetchResellerImage.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchResellerImage.fulfilled, (state, action) => {
			state.loading = false;
			state.resellerImage = action.payload; 
		  })
		  .addCase(fetchResellerImage.rejected, (state) => {
			state.loading = false;
			state.resellerImage = ''; 
		  })

		  
	}
	
	
});

export default resellerSlice.reducer;
export const {  } = resellerSlice.actions;
