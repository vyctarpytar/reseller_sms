import { createSlice, createAsyncThunk } from "@reduxjs/toolkit"; 
import axios from "axios"; 
import axiosInstance from "../../instance";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
	loading: false,
	smsTemplateData: [],  
};

export const fetchSmsTemplates = createAsyncThunk(
	"smsTemplateSlice/fetchSmsTemplates",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/sms/distinct-statuses`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );

   
 
 

export const smsTemplateSlice = createSlice({
	name: 'smsTemplateSlice',
	initialState,
	reducers: {
		 
	},
	extraReducers: (builder) => {
		builder

		.addCase(fetchSmsTemplates.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchSmsTemplates.fulfilled, (state, action) => {
			state.loading = false;
			state.smsTemplateData = action.payload; 
		  })
		  .addCase(fetchSmsTemplates.rejected, (state) => {
			state.loading = false;
			state.smsTemplateData = []; 
		  }) 
		  
	}
	
	
});

export default smsTemplateSlice.reducer;
export const {  } = smsTemplateSlice.actions;
