import { createSlice, createAsyncThunk } from "@reduxjs/toolkit"; 
import axios from "axios"; 
import axiosInstance from "../../instance";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
	loading: false,  
	singleRequest:{},
	refetch: false,
	refetchKey: '', 
	completedCount:0,
	accNoId:[],
	activeData:[],
	activeCount:0,
};
 
 

  export const fetchAccountWithoutId = createAsyncThunk(
	"resellerCodesSlice/fetchAccountWithoutId",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/account/without-sender-id`)
		.then((res) => res.data?.data?.result); 
	  return res;
	}
  );

  export const fetcActiveProductRequest = createAsyncThunk(
	"productRequestSlice/fetcActiveProductRequest",
	async (data) => {
	  const res = await axiosInstance
		.get(`${url}/api/v2/req?reqStatus=${data?.reqStatus}`)
		.then((res) => res.data);   
	  return res;
	}
  );
 
export const resellerCodesSlice = createSlice({
	name: 'reseller-codes',
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

		  .addCase(fetchAccountWithoutId.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetchAccountWithoutId.fulfilled, (state, action) => {
			state.loading = false;
			state.accNoId = action.payload; 
		  })
		  .addCase(fetchAccountWithoutId.rejected, (state) => {
			state.loading = false;
			state.accNoId = []; 
		  })

		  .addCase(fetcActiveProductRequest.pending, (state) => {
			state.loading = true;
		  })
		  .addCase(fetcActiveProductRequest.fulfilled, (state, action) => {
			state.loading = false;
			state.activeData = action.payload?.data?.result; 
			state.activeCount =  action.payload?.total
		  })
		  .addCase(fetcActiveProductRequest.rejected, (state) => {
			state.loading = false;
			state.activeData = []; 
			state.activeCount = 0
		  })
		  
	}
	
	
});

export default resellerCodesSlice.reducer;
export const { setRefetch,setRefetchKey,cleanProductRequest } = resellerCodesSlice.actions;
