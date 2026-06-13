import axios from "axios";
import LoginModal from "./components/LoginModal";

const url = import.meta.env.VITE_API_BASE_URL;

export const handleLogout = async () => {
  await localStorage.clear();
  let open = true;

  function handleCancel() {
    open = false;
  }
  return <LoginModal open={open} handleCancel={handleCancel} />;
};

const axiosInstance = axios.create({
  baseURL: url,
  headers: {
    "Content-Type": "application/json",
  },
});

axiosInstance.interceptors.request.use((config) => {
  const selectedOrg = localStorage.getItem("selectedOrg") || null;
  const selectedAccount = localStorage.getItem("selectedAccount") || null;

  // Stamp every request with the globally selected tenant (reseller_id / account_id)
  // as a DEFAULT only. A caller that sets these explicitly — e.g. the wallet
  // statement's own reseller/account filters — must win, so we don't overwrite a
  // value the caller already provided. (Overwriting was why those filters never
  // reached the backend: localStorage was empty, so the explicit ids became null
  // and axios dropped the null query params.)
  const params = { ...(config.params || {}) };
  if (params.reseller_id == null && selectedOrg) params.reseller_id = selectedOrg;
  if (params.account_id == null && selectedAccount) params.account_id = selectedAccount;
  config.params = params;

  return config;
});

axiosInstance.interceptors.response.use(
  (response) => {
    // console.log(response.headers);

    return response;
  },
  (error) => {
    const statusCode = error.response ? error.response.status : null;

    if (statusCode == 403) {
      handleLogout();
    }

    if (statusCode == 401) {
      handleLogout();
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;
