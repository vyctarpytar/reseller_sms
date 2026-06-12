import { createSlice, createAsyncThunk } from "@reduxjs/toolkit"; 
import axios from "axios"; 
import axiosInstance from "../../instance";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
	loading: false,
	sandData: [],  
};

export const fetchSandbox = createAsyncThunk(
	"sandboxSlice/fetchSandbox",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/api-key`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );

   

export const sandboxSlice = createSlice({
	name: 'sandbox',
	initialState,
	reducers: {
		 
	},
	extraReducers: (builder) => {
		builder

		.addCase(fetchSandbox.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchSandbox.fulfilled, (state, action) => {
			state.loading = false;
			state.sandData = action.payload; 
		  })
		  .addCase(fetchSandbox.rejected, (state) => {
			state.loading = false;
			state.sandData = []; 
		  })

		   
	}
	
	
});

export default sandboxSlice.reducer;
export const {  } = sandboxSlice.actions;
