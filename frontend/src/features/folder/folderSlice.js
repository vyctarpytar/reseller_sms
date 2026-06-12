import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axios from 'axios';
import axiosInstance from "../../instance";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
	fldLoading: false,
	folders: [],
	gradFolders: [],
	folderObj: {},
	otherFolders: [],
	deleteLoading:false,
};

export const saveFolder = createAsyncThunk(
	'folderSlice/saveFolder',
	async (data) => {
		const res = await axiosInstance.post(`${url}/nea/saveFolder.action`, data);
		return res.data;
	}
);

export const fetchFolders = createAsyncThunk(
	'folderSlice/fetchFolders',
	async (instId) => {
		const res = await axiosInstance.get(
			`${url}/nea/fetchFolders.action?fldInstId=${instId}&fldType=EMPLOYEES`
		);
		return res.data.data.result;
	}
);

export const fetchOtherFolders = createAsyncThunk(
	'folderSlice/fetchOtherFolders',
	async (instId) => {
		const res = await axiosInstance.get(
			`${url}/nea/fetchFolders.action?fldInstId=${instId}`
		);
		return res.data.data.result;
	}
);

export const fetchContFolders = createAsyncThunk(
	'folderSlice/fetchContFolders',
	async (instId) => {
		const res = await axiosInstance.get(
			`${url}/api/v2/groups`
		);
		return res.data.data.result;
	}
);

export const deleteGroup = createAsyncThunk(
	'graduate/deleteGroup',
	async (id) => {
		const res = await axiosInstance.delete(
			`${url}/api/v2/groups/${id}`
		); 
		return res?.data;
	}
);

 

export const folderSlice = createSlice({
	name: 'folder',
	initialState,
	reducers: {
		setFolderObj: (state, action) => {
			state.folderObj = action.payload;
		},

		folderCleanUp: (state) => {
			state.folderObj = {};
		},
	},
	extraReducers: (builder) => {
		builder

			.addCase(saveFolder.pending, (state) => {
				state.fldLoading = true;
			})
			.addCase(saveFolder.fulfilled, (state) => {
				state.fldLoading = false;
			})
			.addCase(saveFolder.rejected, (state) => {
				state.fldLoading = false;
			})

			.addCase(fetchContFolders.pending, (state) => {
				state.fldLoading = true;
			})
			.addCase(fetchContFolders.fulfilled, (state, action) => {
				state.fldLoading = false;
				state.gradFolders = action?.payload;
			})
			.addCase(fetchContFolders.rejected, (state) => {
				state.fldLoading = false;
				state.gradFolders = [];
			})

			.addCase(fetchFolders.pending, (state) => {
				state.fldLoading = true;
			})
			.addCase(fetchFolders.fulfilled, (state, action) => {
				state.fldLoading = false;
				state.folders = action?.payload;
			})
			.addCase(fetchFolders.rejected, (state) => {
				state.fldLoading = false;
				state.folders = [];
			})

			.addCase(fetchOtherFolders.pending, (state) => {
				state.fldLoading = true;
			})
			.addCase(fetchOtherFolders.fulfilled, (state, action) => {
				state.fldLoading = false;
				state.otherFolders = action?.payload?.filter(
					(item) => item?.fldType === null
				);
			})
			.addCase(fetchOtherFolders.rejected, (state) => {
				state.fldLoading = false;
				state.otherFolders = [];
			})

			.addCase(deleteGroup.pending, (state) => {
				state.deleteLoading = true;
			})
			.addCase(deleteGroup.fulfilled, (state, action) => {
				state.deleteLoading = false; 
			})
			.addCase(deleteGroup.rejected, (state) => {
				state.deleteLoading = false; 
			})



			
	},
});

export default folderSlice.reducer;
export const { setFolderObj, folderCleanUp } = folderSlice.actions;
