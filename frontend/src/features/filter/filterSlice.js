import { createSlice, createAsyncThunk } from "@reduxjs/toolkit"; 
import axios from "axios"; 
import axiosInstance from "../../instance";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
	loading: false,
	statusData: [], 
	invoiceStatusData:[],
	accountStatusData:[],
	senderNamesData:[],
	senderIdStatus:[],
	creditStatus:[],
};

export const fetchDistinctStatus = createAsyncThunk(
	"filterSlice/fetchDistinctStatus",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/sms/distinct-statuses`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );

  export const fetchInvoiceStatus = createAsyncThunk(
	"filterSlice/fetchInvoiceStatus",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/credit/distinct-invo-statuses`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );

  export const fetchAccountStatus = createAsyncThunk(
	"filterSlice/fetchAccountStatus",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/account/distionct-status`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );

  export const fetchDistinctSenderNames = createAsyncThunk(
	"filterSlice/fetchDistinctSenderNames",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/setup/distinct-sender-names`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );
 

  export const fetchSenderIdStatus = createAsyncThunk(
	"filterSlice/fetchSenderIdStatus",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/setup/distinct-status`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );
 

  export const fetchUsersStatus = createAsyncThunk(
	"filterSlice/fetchUsersStatus",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/users/distinct-status`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );
 
  export const fetchCreditStatus = createAsyncThunk(
	"filterSlice/fetchCreditStatus",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/credit/distinct-status`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );

export const filterSlice = createSlice({
	name: 'filterSlice',
	initialState,
	reducers: {
		 
	},
	extraReducers: (builder) => {
		builder

		.addCase(fetchDistinctStatus.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchDistinctStatus.fulfilled, (state, action) => {
			state.loading = false;
			state.statusData = action.payload; 
		  })
		  .addCase(fetchDistinctStatus.rejected, (state) => {
			state.loading = false;
			state.statusData = []; 
		  })


		  .addCase(fetchInvoiceStatus.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchInvoiceStatus.fulfilled, (state, action) => {
			state.loading = false;
			state.invoiceStatusData = action.payload; 
		  })
		  .addCase(fetchInvoiceStatus.rejected, (state) => {
			state.loading = false;
			state.invoiceStatusData = []; 
		  })

		   
		  .addCase(fetchAccountStatus.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchAccountStatus.fulfilled, (state, action) => {
			state.loading = false;
			state.accountStatusData = action.payload; 
		  })
		  .addCase(fetchAccountStatus.rejected, (state) => {
			state.loading = false;
			state.accountStatusData = []; 
		  })

		  .addCase(fetchDistinctSenderNames.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchDistinctSenderNames.fulfilled, (state, action) => {
			state.loading = false;
			state.senderNamesData = action.payload; 
		  })
		  .addCase(fetchDistinctSenderNames.rejected, (state) => {
			state.loading = false;
			state.senderNamesData = []; 
		  })

		  .addCase(fetchSenderIdStatus.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchSenderIdStatus.fulfilled, (state, action) => {
			state.loading = false;
			state.senderIdStatus = action.payload; 
		  })
		  .addCase(fetchSenderIdStatus.rejected, (state) => {
			state.loading = false;
			state.senderIdStatus = []; 
		  })

		  .addCase(fetchUsersStatus.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchUsersStatus.fulfilled, (state, action) => {
			state.loading = false;
			state.userStatus = action.payload; 
		  })
		  .addCase(fetchUsersStatus.rejected, (state) => {
			state.loading = false;
			state.userStatus = []; 
		  })

		  .addCase(fetchCreditStatus.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchCreditStatus.fulfilled, (state, action) => {
			state.loading = false;
			state.creditStatus = action.payload; 
		  })
		  .addCase(fetchCreditStatus.rejected, (state) => {
			state.loading = false;
			state.creditStatus = []; 
		  })
		  

		  
		  
	}
	
	
});

export default filterSlice.reducer;
export const {  } = filterSlice.actions;
