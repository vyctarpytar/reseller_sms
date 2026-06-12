import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import { message } from "antd";
import axios from "axios";
import axiosInstance from "../../instance";
import { BASE_URL } from "../../config/constant";

const url = process.env.REACT_APP_API_BASE_URL;

const initialState = {
  authLoading: false,
  user: {},
  isLoggedIn: false,
  token: "",
  users: [],
  current: 0,
  myUsersData: [],
  usersLoading: false,
  permissionData: [],
  loading:false,
  forgotAcc:{},
  saving: false,
  singleUser:{},
  rolesData: [],
  userProfile:[],
  usersCount:0,
};

export const save = createAsyncThunk(
  "user/save",
  async (data, { rejectWithValue }) => {
    let saveUrl = data.url;
    delete data.url;
    try {
      const response = await axios.post(`${url}/${saveUrl}`, data);
      if (!response.data.success) {
        return rejectWithValue(response.data);
      }
      return response.data;
    } catch (error) {
      return rejectWithValue(error.response.data);
    }
  }
);

export const login = createAsyncThunk("authSlice/login", async (data) => {
  const res = await axios
    .post(`${url}/auth/login`, data)
    .then((res) => res?.data);
  return res;
});

 

export const fetchMyUsers = createAsyncThunk('saveSlice/fetch/fetchMyUsers', async (data, { rejectWithValue }) => {
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

export const fetchPermissions = createAsyncThunk(
  "user/fetchPermissions",
  async (data) => {
    const res = await axiosInstance
      .get(`${url}/api/v2/users/permission/${data?.role}`)
      .then((res) => res.data?.data?.result);
    return res;
  }
);
 
export const resetPassword = createAsyncThunk('user/resetPassword', async (data, { rejectWithValue }) => {
  let saveUrl = data.url;
  delete data.url;
  try {
      const response = await axios.patch(`${url}/${saveUrl}`, data)
      if (!response.data.success) {
          return rejectWithValue(response.data);
      }   
      return response.data;
  } catch (error) { 
      return rejectWithValue(error.response.data);
  }
});



export const resendOtp = createAsyncThunk(
  "authSlice/resendOtp",
  async (data) => {
    const res = await axios
      .post(`${url}/auth/resendOtp.action`, data)
      .then((res) => res.data);
    return res;
  }
);

export const verifyOtp = createAsyncThunk(
  "authSlice/verifyOtp",
  async (data) => {
    const res = await axios
      .post(`${url}/auth/verifyOtp.action`, data)
      .then((res) => res.data);
    return res;
  }
);

export const updateUser = createAsyncThunk(
  "authSlice/updateUser",
  async (data) => {
    const res = await axios
      .post(`${url}/auth/saveUser.action`, data)
      .then((res) => res.data);
    return res;
  }
);

export const saveOtp = createAsyncThunk('saveSlice/saveOtp', async (data) => {
	let saveUrl = data.url;
	delete data.url;
	const res = await axios
		.post(`${url}/${saveUrl}`, data)
		.then((res) => res?.data);
	return res;
});


export const fetchRoles = createAsyncThunk(
  "user/fetchRoles",
  async (data) => {
    const res = await axiosInstance
      .get(`${url}/api/v2/users/roles`)
      .then((res) => res.data?.data?.result);
    return res;
  }
);

export const fetchMyProfile = createAsyncThunk(
  "user/fetchMyProfile",
  async (data) => {
    const res = await axiosInstance
      .get(`${url}/api/v2/users/me`) 
      .then((res) => res.data?.data?.response);
    return res;
  }
);


export const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    logout: (state) => {
      state.user = {};
      state.isLoggedIn = false;
      state.token = "";
      localStorage.clear();
    },
    setToken: (state, action) => {
      state.token = action.payload;
      state.isLoggedIn = true;
    },

    setUser: (state, action) => {
      state.user = action.payload;
    },
    setIsLoggedIn: (state, action) => {
      state.isLoggedIn = action.payload;
    },
    cleanForgotAcc: (state, action) => {
      state.forgotAcc = initialState.forgotAcc;
    },
    cleanSingleUser:(state)=>{
      state.singleUser = initialState.singleUser; 
    },
    cleanAuthLoading:(state)=>{
      state.authLoading = initialState.authLoading;
    },
    clearAuthObj: () => {
      return { ...initialState };
  },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.authLoading = true;
      })
      .addCase(login.fulfilled, (state, action) => {
        state.authLoading = false;
        state.user = action?.payload?.messages?.message;
        state.isLoggedIn = true;
      })
      .addCase(login.rejected, (state) => {
        state.authLoading = false;
        state.isLoggedIn = false;
      })

      .addCase(fetchMyUsers.pending, (state) => {
        state.usersLoading = true;
      })
      .addCase(fetchMyUsers.fulfilled, (state, action) => {
        state.usersLoading = false;
        state.myUsersData = action.payload?.data?.result;
        state.usersCount =  action.payload.total
      })
      .addCase(fetchMyUsers.rejected, (state) => {
        state.usersLoading = false;
        state.myUsersData = [];
        state.usersCount = 0 
      })

      .addCase(fetchPermissions.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchPermissions.fulfilled, (state, action) => {
        state.loading = false;
        state.permissionData = action.payload?.[0];
      })
      .addCase(fetchPermissions.rejected, (state) => {
        state.loading = false;
        state.permissionData = [];
      })

      .addCase(verifyOtp.pending, (state) => {
        state.authLoading = true;
      })
      .addCase(verifyOtp.fulfilled, (state, action) => {
        state.authLoading = false;
        state.user = action.payload?.jsonData;
      })
      .addCase(verifyOtp.rejected, (state) => {
        state.authLoading = false;
        state.user = {};
      })

      .addCase(updateUser.pending, (state) => {
        state.authLoading = true;
      })
      .addCase(updateUser.fulfilled, (state, action) => {
        state.authLoading = false;
      })
      .addCase(updateUser.rejected, (state) => {
        state.authLoading = false;
      })


      .addCase(resendOtp.pending, (state) => {
        state.authLoading = true;
      })
      .addCase(resendOtp.fulfilled, (state, action) => {
        state.authLoading = false;
        state.user = action.payload?.jsonData;
      })
      .addCase(resendOtp.rejected, (state) => {
        state.authLoading = false;
        state.user = {};
      })
 
      .addCase(saveOtp.pending, (state) => {
				state.saving = true;
			})
			.addCase(saveOtp.fulfilled, (state, action) => {
				state.saving = false; 
			})
			.addCase(saveOtp.rejected, (state) => {
				state.saving = false; 
			})

      .addCase(resetPassword.pending, (state) => {
				state.saving = true;
			})
			.addCase(resetPassword.fulfilled, (state, action) => {
				state.saving = false; 
        state.forgotAcc =  action.payload?.data?.result
			})
			.addCase(resetPassword.rejected, (state) => {
				state.saving = false; 
			})

      .addCase(save.pending, (state) => {
				state.saving = true;
			})
			.addCase(save.fulfilled, (state, action) => {
				state.saving = false; 
        state.singleUser=action?.payload?.data?.result
			})
			.addCase(save.rejected, (state) => { 
				state.saving = false; 
			})


      .addCase(fetchRoles.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchRoles.fulfilled, (state, action) => {
        state.loading = false;
        state.rolesData = action.payload;
      })
      .addCase(fetchRoles.rejected, (state) => {
        state.loading = false;
        state.rolesData = [];
      })

      .addCase(fetchMyProfile.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchMyProfile.fulfilled, (state, action) => {
        state.loading = false;
        state.userProfile = action.payload?.[0];
      })
      .addCase(fetchMyProfile.rejected, (state) => {
        state.loading = false;
        state.userProfile = [];
      })

      
  },
});

export default authSlice.reducer;
export const { logout, setToken, setUser, setIsLoggedIn,cleanForgotAcc,
  cleanSingleUser,cleanAuthLoading,clearAuthObj
 } = authSlice.actions;
