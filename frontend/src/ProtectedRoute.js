import {
  Navigate,
  Outlet,
  useLocation,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import SideBar from "./layout/SideBar";
import SubHeader from "./layout/SubHeader";
import { fetchAccountBalance, fetchMenu, fetchResellerBalance, fetchTopBalance } from "./features/menu/menuSlice";
import { jwtDecode } from "jwt-decode";
import { logout, setIsLoggedIn, setUser } from "./features/auth/authSlice";
import { useEffect } from "react";
import axiosInstance from "./instance";
import { cleanCurrent } from "./features/sms-request/smsRequestSlice";

export const ProtectedRoute = ({ role }) => {
  const { isLoggedIn, token } = useSelector((state) => state.auth);
  const { menuData } = useSelector((state) => state.menu);
  const { user } = useSelector((state) => state.auth);
  const { pathname } = useLocation();
  const dispatch = useDispatch();
  const navigate = useNavigate();

  axiosInstance.defaults.headers.common["Authorization"] = `Bearer ${token}`;

  const isTokenExpired = (token) => {
    if (!token) return true;

    const decodedToken = token?.length && jwtDecode(token);
    const currentTime = Date.now() / 1000;

    if (decodedToken?.exp < currentTime) {
      return true;
    }

    return false;
  };
  async function clean() {
    await dispatch(cleanCurrent());
  }

  async function handleInit() {
		if (token?.length > 0) {
			const decoded = await jwtDecode(token);
			await dispatch(setUser(decoded));
		} else { 
			return;
		}
	}

  async function handleLogout() {  
    await dispatch(logout()); 
    await localStorage.clear();
    await dispatch(setIsLoggedIn(false));
    await navigate("/login");
    await clean();
  }
 
  // async function fetchBalanceData(){
  //   console.log("protected ")
  //   if(user?.layer === "RESELLER"){
  //     console.log("entering RESELLER")
  //     dispatch(fetchResellerBalance())
  //   }
  //   if(user?.layer === "ACCOUNT"){
  //     console.log("entering ACCOUNT")
  //     dispatch(fetchAccountBalance())
  //   } 
  //   if (user?.layer === "TOP") {
  //     console.log("entering top")
  //     dispatch(fetchTopBalance());
  //   }
    
  // }
 
  useEffect(()=>{
    // if(user?.layer === "RESELLER"){
    //   navigate('/dashboard-reseller')
    // }
    // if(user?.layer === "ACCOUNT"){
    //   navigate('/dashboard-account')
    // }
    if(user?.changePassword === true || user?.changePassword === null){
      navigate('/account-settings')
    }
  },[user])

  // useEffect(()=>{
  //   fetchBalanceData()
  // },[])


  useEffect(() => {
    if (isTokenExpired(token) || !menuData || menuData?.length === 0) {
      handleLogout();
    }
  }, [token, pathname, menuData]);

  useEffect(()=>{
    clean();
  },[pathname])

  useEffect(()=>{ 
    handleInit();
  },[])

  
  if (!isLoggedIn) {
    return <Navigate to="/login" replace />
  }

  return (
    <>
      
        <div className="flex items-start justify-start w-full h-[92vh]">
          <SideBar />
          <Outlet />
        </div> 
    </>
  );
};
