import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axiosInstance from '../../instance';

const url = import.meta.env.VITE_API_BASE_URL;

const initialState = {
	saving: false,
	singleloggedInUser:{},
	sentSmsData:[],
	loadingSms: false,
	sentSmsCount:0,
	sentSmsSummary:null,
	loadingSummary:false,
	senderIdData:[],
	senderIdCount:0,
	loadingId:false,
	templatesData:[],
	templateCount:0,
	loading:false,
	scheduledSmsData:[],
	scheduledSmsCount:0,
};

// export const save = createAsyncThunk('saveSlice/save', async (data) => {
// 	let saveUrl = data.url;
// 	delete data.url;
// 	const res = await axiosInstance
// 		.post(`${url}${saveUrl}`, data)
// 		.then((res) => res.data);
// 	return res;
// });

export const saveForget = createAsyncThunk('saveSlice/save', async (data, { rejectWithValue }) => {
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

  export const save = createAsyncThunk('saveSlice/save/ordinary', async (data, { rejectWithValue }) => {
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

  export const saveArray = createAsyncThunk(
  "saveSlice/saveArray/ordinary",
  async (data, { rejectWithValue }) => {
    let saveUrl = data.url;
    delete data.url;
    await new Promise((resolve) => setTimeout(resolve, 0));
    const payloadToSend = Array.isArray(data.data) ? data.data : data;

    try {
      const response = await axiosInstance.post(`/${saveUrl}`, payloadToSend);
      if (!response.data.success) {
        return rejectWithValue(response.data);
      }
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response.data);
    }
  }
);

  export const deleteRequest = createAsyncThunk(
  "deleteSlice/delete/ordinary",
  async (data, { rejectWithValue }) => {
    let deleteUrl = data.url;
    delete data.url; 

    try {
      const response = await axiosInstance.delete(`/${deleteUrl}`, {
        data, 
      });

      if (!response.data.success) {
        return rejectWithValue(response.data);
      }
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);


export const saveFile = createAsyncThunk('saveSlice/saveFile', async (data) => {
	const res = await axiosInstance
		.post(`${url}/api/v2/req/file-upload`, data, {
			headers: {
				'content-type': 'multipart/form-data'
			}
		})
		.then((res) => res.data);
	return res;
});

export const saveExcel = createAsyncThunk('saveSlice/saveExcel', async (data) => {
	const res = await axiosInstance
		.post(`${url}/api/v2/member/upload`, data, {
			headers: {
				'content-type': 'multipart/form-data'
			}
		})
		.then((res) => res.data);
	return res;
});

export const saveSmsCsv = createAsyncThunk('saveSlice/saveSmsCsv', async (data) => {
	const res = await axiosInstance
		.post(`${url}/api/v2/sms/upload-csv`, data, {
			headers: {
				'content-type': 'multipart/form-data'
			}
		})
		.then((res) => res.data);
	return res;
});

export const fetchSavedSms = createAsyncThunk('saveSlice/fetch/saved/sms', async (data, { rejectWithValue }) => {
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

  // Server-aggregated summary for the Sent SMS screen. Hits the same dashboard
  // aggregation endpoint (api/v2/dash) so totals stay accurate and account-scoped
  // (the axios interceptor injects reseller_id/account_id) instead of summing a page.
  export const fetchSentSmsSummary = createAsyncThunk(
	'saveSlice/fetch/sent-sms/summary',
	async (data, { rejectWithValue }) => {
		let saveUrl = data.url;
		delete data.url;
		try {
			const response = await axiosInstance.post(`${url}/${saveUrl}`, data);
			if (!response.data.success) {
				return rejectWithValue(response.data);
			}
			return response.data;
		} catch (error) {
			return rejectWithValue(error.response?.data);
		}
	}
  );

  export const downloadExcel = createAsyncThunk(
	"sms/downloadExcel",
	async (data, { rejectWithValue }) => {
	let saveUrl = data.url;
	delete data.url;
	  try {
		const response = await axiosInstance.post(`${url}/${saveUrl}`, data, {
		  responseType: "blob",
		});
		return response.data;
	  } catch (error) {
		return rejectWithValue(error.response.data);
	  }
	}
  );

  export const fetchSenderIds = createAsyncThunk('saveSlice/fetch/fetchSenderIds', async (data, { rejectWithValue }) => {
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

  export const fetchTemplates = createAsyncThunk('saveSlice/fetch/fetchTemplates', async (data, { rejectWithValue }) => {
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

  export const fetchScheduledSms = createAsyncThunk('saveSlice/fetch/saved/fetchScheduledSms', async (data, { rejectWithValue }) => {
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

export const saveSlice = createSlice({
	name: 'save',
	initialState,
	reducers: {
		cleanSingleloggedInUser:(state,action)=>{
			state.singleloggedInUser= initialState.singleloggedInUser
		}
	},
	extraReducers: (builder) => {
		builder

			.addCase(saveForget.pending, (state) => {
				state.saving = true;
			})
			.addCase(saveForget.fulfilled, (state, action) => {
				state.saving = false;
				state.singleloggedInUser =  action.payload?.data?.result
			})
			.addCase(saveForget.rejected, (state) => {
				state.saving = false;
			})

			.addCase(save.pending, (state) => {
				state.saving = true;
			})
			.addCase(save.fulfilled, (state, action) => {
				state.saving = false; 
			})
			.addCase(save.rejected, (state) => {
				state.saving = false;
			})


			
 
			.addCase(deleteRequest.pending, (state) => {
				state.saving = true;
			})
			.addCase(deleteRequest.fulfilled, (state, action) => {
				state.saving = false; 
			})
			.addCase(deleteRequest.rejected, (state) => {
				state.saving = false;
			})

			

			.addCase(saveFile.pending, (state) => {
				state.saving = true;
			})
			.addCase(saveFile.fulfilled, (state, action) => {
				state.saving = false;
			})
			.addCase(saveFile.rejected, (state) => {
				state.saving = false;
			})

			.addCase(saveExcel.pending, (state) => {
				state.saving = true;
			})
			.addCase(saveExcel.fulfilled, (state, action) => {
				state.saving = false;
			})
			.addCase(saveExcel.rejected, (state) => {
				state.saving = false;
			})

			.addCase(saveSmsCsv.pending, (state) => {
				state.saving = true;
			})
			.addCase(saveSmsCsv.fulfilled, (state, action) => {
				state.saving = false;
			})
			.addCase(saveSmsCsv.rejected, (state) => {
				state.saving = false;
			})

			.addCase(fetchSavedSms.pending, (state) => {
				state.loadingSms = true;
			})
			.addCase(fetchSavedSms.fulfilled, (state, action) => {
				state.loadingSms = false;
				state.sentSmsData= action.payload?.data?.result;
				state.sentSmsCount =  action.payload?.total
			})
			.addCase(fetchSentSmsSummary.pending, (state) => {
				state.loadingSummary = true;
			})
			.addCase(fetchSentSmsSummary.fulfilled, (state, action) => {
				state.loadingSummary = false;
				state.sentSmsSummary = action.payload?.data?.result;
			})
			.addCase(fetchSentSmsSummary.rejected, (state) => {
				state.loadingSummary = false;
				state.sentSmsSummary = null;
			})
			.addCase(fetchSavedSms.rejected, (state) => {
				state.loadingSms = false;
				state.sentSmsData = [];
				state.sentSmsCount = 0
			})

			.addCase(downloadExcel.pending, (state) => {
				state.saving = true;
			})
			.addCase(downloadExcel.fulfilled, (state, action) => {
				state.saving = false;
			})
			.addCase(downloadExcel.rejected, (state) => {
				state.saving = false;
			})
			

			.addCase(fetchSenderIds.pending, (state) => {
				state.loadingId = true;
			})
			.addCase(fetchSenderIds.fulfilled, (state, action) => {
				state.loadingId = false;
				state.senderIdData= action.payload?.data?.result;
				state.senderIdCount =  action.payload?.total
			})
			.addCase(fetchSenderIds.rejected, (state) => {
				state.loadingId = false;
				state.senderIdData = [];
				state.senderIdCount = 0
			})

			.addCase(fetchTemplates.pending, (state) => {
				state.loading = true;
			})
			.addCase(fetchTemplates.fulfilled, (state, action) => {
				state.loading = false;
				state.templatesData= action.payload?.data?.result;
				state.templateCount =  action.payload?.total
			})
			.addCase(fetchTemplates.rejected, (state) => {
				state.loading = false;
				state.templatesData = [];
				state.templateCount = 0
			})


			.addCase(fetchScheduledSms.pending, (state) => {
				state.loading = true;
			})
			.addCase(fetchScheduledSms.fulfilled, (state, action) => {
				state.loading = false;
				state.scheduledSmsData= action.payload?.data?.result;
				state.scheduledSmsCount =  action.payload?.total
			})
			.addCase(fetchScheduledSms.rejected, (state) => {
				state.loading = false;
				state.scheduledSmsData = [];
				state.scheduledSmsCount = 0
			})


			
			


			
	},
});

export default saveSlice.reducer;
export const {cleanSingleloggedInUser} = saveSlice.actions;
