import axios from "axios";
import LoginModal from "./components/LoginModal";

const url = process.env.REACT_APP_API_BASE_URL;

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

  config.params = {
    ...(config.params || {}),
    reseller_id: selectedOrg,
    account_id: selectedAccount,
  };

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
