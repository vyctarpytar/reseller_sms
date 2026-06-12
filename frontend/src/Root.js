import { Outlet, useLocation, useNavigate } from "react-router-dom";
import Header from "./layout/Header";
import { useDispatch, useSelector } from "react-redux";
import { jwtDecode } from "jwt-decode";
import { useEffect, useState } from "react";
import { cleanAuthLoading, logout } from "./features/auth/authSlice";
import { cleanBalanceHeader, fetchMenu } from "./features/menu/menuSlice";

export default function Root() {
  const { isLoggedIn, user, token } = useSelector((state) => state.auth);

  const [tokenExpired, setTokenExpired] = useState(false);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const loc = useLocation();
  const isTokenExpired = (token) => {
    if (!token) return true;

    const decodedToken = token?.length && jwtDecode(token);
    const currentTime = Date.now() / 1000;

    return decodedToken.exp < currentTime;
  };

  useEffect(() => {
    if (!token) {
      setTokenExpired(true);
      dispatch(logout());
      dispatch(cleanBalanceHeader());
      dispatch(cleanAuthLoading());
      navigate("/login");
      return;
    }

    if (isTokenExpired(token)) {
      setTokenExpired(true);
      dispatch(logout());
      dispatch(cleanBalanceHeader());
      dispatch(cleanAuthLoading());
      navigate("/login");
    } else {
      setTokenExpired(false);
    }
  }, [token, dispatch, navigate]);
  const orgId = localStorage.getItem("selectedOrg");
  const accId = localStorage.getItem("selectedAccount");

  useEffect(() => {
    if (user) {
      dispatch(fetchMenu());
    }
  }, [user, orgId, accId]);

  return (
    <div className="flex flex-col w-full ">
      {isLoggedIn &&
        !tokenExpired &&
        window.location.href != "/" &&
        loc?.pathname != "/" && <Header />}

      <div className="flex flex-col items-start w-full h-full">
        <Outlet />
      </div>
    </div>
  );
}
