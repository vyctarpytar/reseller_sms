import { createSlice, createAsyncThunk } from "@reduxjs/toolkit"; 
import axios from "axios"; 
import axiosInstance from "../../instance";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
	loading: false,
	newData: [],
	progressData: [],
	approvedData:[],
	singleRequest:{},
	refetch: false,
	refetchKey: '',
	newCount:0,
	inProgressCount:0,
	completedCount:0,
};

export const fetchNewProductRequest = createAsyncThunk(
	"productRequestSlice/fetchNewProductRequest",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/req?reqStatus=${data?.reqStatus}`)
		.then((res) => res.data); 
	  return res;
	}
  );

  export const fetchInProgressProductRequest = createAsyncThunk(
	"productRequestSlice/fetchInProgressProductRequest",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/req?reqStatus=${data?.reqStatus}`)
		.then((res) => res.data); 
	  return res;
	}
  );

  export const fetchApprovedProductRequest = createAsyncThunk(
	"productRequestSlice/fetchApprovedProductRequest",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/req?reqStatus=${data?.reqStatus}`)
		.then((res) => res.data);   
	  return res;
	}
  );

  export const fetchSingleRequest = createAsyncThunk(
	"productRequestSlice/fetchSingleRequest",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/req/${data?.reqId}`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );
 
export const productRequestSlice = createSlice({
	name: 'product-request',
	initialState,
	reducers: {
		setRefetch: (state) => {
			state.refetch = !state.refetch;
		},
		setRefetchKey: (state, action) => {
			state.refetchKey = action.payload;
		},

		cleanProductRequest: (state) => { 
			state.refetchKey = initialState.refetchKey; 
		},
	},
	extraReducers: (builder) => {
		builder

		.addCase(fetchNewProductRequest.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchNewProductRequest.fulfilled, (state, action) => {
			state.loading = false;
			state.newData = action.payload?.data?.result; 
			state.newCount =  action.payload?.total
		  })
		  .addCase(fetchNewProductRequest.rejected, (state) => {
			state.loading = false;
			state.newData = []; 
			state.newCount = 0
		  })

		  .addCase(fetchInProgressProductRequest.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchInProgressProductRequest.fulfilled, (state, action) => {
			state.loading = false;
			state.progressData = action.payload?.data?.result; 
			state.inProgressCount =  action.payload?.total
		  })
		  .addCase(fetchInProgressProductRequest.rejected, (state) => {
			state.loading = false;
			state.progressData = []; 
			state.inProgressCount = 0
		  })

		  .addCase(fetchApprovedProductRequest.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchApprovedProductRequest.fulfilled, (state, action) => {
			state.loading = false;
			state.approvedData = action.payload?.data?.result; 
			state.completedCount =  action.payload?.total
		  })
		  .addCase(fetchApprovedProductRequest.rejected, (state) => {
			state.loading = false;
			state.approvedData = []; 
			state.completedCount =  0;
		  })

		  .addCase(fetchSingleRequest.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchSingleRequest.fulfilled, (state, action) => {
			state.loading = false;
			state.singleRequest = action.payload; 
		  })
		  .addCase(fetchSingleRequest.rejected, (state) => {
			state.loading = false;
			state.singleRequest = []; 
		  })

		  
	}
	
	
});

export default productRequestSlice.reducer;
export const { setRefetch,setRefetchKey,cleanProductRequest } = productRequestSlice.actions;
