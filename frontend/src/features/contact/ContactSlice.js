import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axios from 'axios';
import axiosInstance from "../../instance";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
	gradLoading: false,
	contacts: [],
	contactsHeaders: [],
	gradHeaderObj: {},
	gradListObj: {},
	graduateObj: {},
	gradUpload: false,
	loading: false,
	singleContact:{},
	deleteLoading:false,
};

export const saveGraduate = createAsyncThunk(
	'graduate/saveGraduate',
	async (data) => {
		const res = await axiosInstance.post(`${url}/emp/saveGraduate.action`, data);
		return res.data;
	}
);

export const saveGraduateHeader = createAsyncThunk(
	'graduate/saveGraduateHeader',
	async (data) => {
		const res = await axiosInstance.post(`${url}/emp/saveGraduateFolder.action`, data);
		return res.data;
	}
);

export const fetchContacts = createAsyncThunk(
	'graduate/fetchContacts',
	async (grpId) => {
		const res = await axiosInstance.get(
			`${url}/api/v2/member/${grpId}`
		);
		return res.data?.data?.result;
	}
);
 

export const fetchContactFolders = createAsyncThunk(
	'graduate/fetchContactFolders',
	async (folderId) => {
		const res = await axiosInstance.get(
			`${url}/api/v2/groups/${folderId}`
		);
		return res.data?.data?.result;
	}
);

export const deleteGraduate = createAsyncThunk(
	'graduate/deleteGraduate',
	async (grdId) => {
		const res = await axiosInstance.get(
			`${url}/emp/deleteGraduate.action?grdId=${grdId}`
		);
		return res.data;
	}
);

 
export const downloadMembersExcel = createAsyncThunk(
	'graduate/downloadMembersExcel',
	async (_, { rejectWithValue }) => {
	  try {
		const response = await axiosInstance.get(
		  `${url}/api/v2/member/download-template`,
		  {
			responseType: 'blob',
		  }
		); 
		return response.data;
	  } catch (error) {
		return rejectWithValue(error.response.data);
	  }
	}
  );

  export const fetchSingleContacts = createAsyncThunk(
	'graduate/fetchSingleContacts',
	async (id) => {
		const res = await axiosInstance.get(
			`${url}/api/v2/member/id/${id}`
		);
		return res.data?.data?.result;
	}
);
  
export const deleteContact = createAsyncThunk(
	'graduate/deleteContact',
	async (id) => {
		const res = await axiosInstance.delete(
			`${url}/api/v2/member/${id}`
		); 
		return res?.data;
	}
);


 
export const ContactSlice = createSlice({
	name: 'contact',
	initialState,
	reducers: {
		setGraduateHeaderObj: (state, action) => {
			state.gradHeaderObj = action.payload;
		},

		setGraduateListObj: (state, action) => {
			state.gradListObj = action.payload;
		},

		setGraduateObj: (state, action) => {
			state.graduateObj = action.payload;
		},

		setGradUpload: (state, action) => {
			state.gradUpload = action.payload;
		},

		graduateCleanUp: (state) => {
			state.gradHeaderObj = {};
			state.gradListObj = {};
			state.graduateObj = {};
			state.gradUpload = false;
		},
	},
	extraReducers: (builder) => {
		builder

			.addCase(deleteGraduate.pending, (state) => {
				state.gradLoading = true;
			})
			.addCase(deleteGraduate.fulfilled, (state) => {
				state.gradLoading = false;
			})
			.addCase(deleteGraduate.rejected, (state) => {
				state.gradLoading = false;
			})

			.addCase(saveGraduateHeader.pending, (state) => {
				state.gradLoading = true;
			})
			.addCase(saveGraduateHeader.fulfilled, (state) => {
				state.gradLoading = false;
			})
			.addCase(saveGraduateHeader.rejected, (state) => {
				state.gradLoading = false;
			})

			.addCase(saveGraduate.pending, (state) => {
				state.gradLoading = true;
			})
			.addCase(saveGraduate.fulfilled, (state) => {
				state.gradLoading = false;
			})
			.addCase(saveGraduate.rejected, (state) => {
				state.gradLoading = false;
			})

			.addCase(fetchContactFolders.pending, (state) => {
				state.gradLoading = true;
			})
			.addCase(fetchContactFolders.fulfilled, (state, action) => {
				state.gradLoading = false;
				state.contactsHeaders = action.payload;
			})
			.addCase(fetchContactFolders.rejected, (state) => {
				state.gradLoading = false;
				state.contactsHeaders = {};
			})

			.addCase(fetchContacts.pending, (state) => {
				state.gradLoading = true;
			})
			.addCase(fetchContacts.fulfilled, (state, action) => {
				state.gradLoading = false;
				state.contacts = action.payload;
			})
			.addCase(fetchContacts.rejected, (state) => {
				state.gradLoading = false;
				state.contacts = {};
			})

			.addCase(downloadMembersExcel.pending, (state) => {
				state.loading = true;
			})
			.addCase(downloadMembersExcel.fulfilled, (state, action) => {
				state.loading = false; 
			})
			.addCase(downloadMembersExcel.rejected, (state) => {
				state.loading = false; 
			})


			.addCase(fetchSingleContacts.pending, (state) => {
				state.gradLoading = true;
			})
			.addCase(fetchSingleContacts.fulfilled, (state, action) => {
				state.gradLoading = false;
				state.singleContact = action.payload;
			})
			.addCase(fetchSingleContacts.rejected, (state) => {
				state.gradLoading = false;
				state.singleContact = {};
			})

			.addCase(deleteContact.pending, (state) => {
				state.deleteLoading = true;
			})
			.addCase(deleteContact.fulfilled, (state, action) => {
				state.deleteLoading = false; 
			})
			.addCase(deleteContact.rejected, (state) => {
				state.deleteLoading = false; 
			})


			

			

			
	},
});

export default ContactSlice.reducer;
export const {
	setGraduateHeaderObj,
	graduateCleanUp,
	setGraduateListObj,
	setGraduateObj,
	setGradUpload,
} = ContactSlice.actions;
