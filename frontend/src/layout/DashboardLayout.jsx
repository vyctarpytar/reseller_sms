import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux"; 
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { 
  Menu, 
} from "antd";
import { handleLoginSession, logout } from "../features/auth/authSlice"; 
import MobileDrawer from "./MobileDrawer"; 
import MaterialIcon from "material-icons-react";
import  dashboardsvg from '../assets/svg/grid.svg'

const url = process.env.REACT_APP_API_BASE_URL;

export default function DashboardLayout({ children }) {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  // const { fullWidth } = useSelector((state) => state.global);
  const { user } = useSelector((state) => state.auth);
  const { notifications } = useSelector((state) => state.global);

  const [open, setOpen] = useState(false);
  const [notOpen, setnotOpen] = useState(false);

  const showDrawer = () => {
    setOpen(true);
  };
  const onClose = () => {
    setOpen(false);
  };

  const dispatch = useDispatch();

  // async function handleLogout(e) {
  //   e.preventDefault();
  //   await dispatch(logout());
  // }
  const handleLogout = () => {
    dispatch(logout());
    navigate("/");
  };

  const items = [
    {
      key: "1",
      label: <Link to="/profile">My Profile</Link>,
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
        >
          <g clipPath="url(#clip0_4865_1210)">
            <path
              d="M12 6C13.1 6 14 6.9 14 8C14 9.1 13.1 10 12 10C10.9 10 10 9.1 10 8C10 6.9 10.9 6 12 6ZM12 16C14.7 16 17.8 17.29 18 18H6C6.23 17.28 9.31 16 12 16ZM12 4C9.79 4 8 5.79 8 8C8 10.21 9.79 12 12 12C14.21 12 16 10.21 16 8C16 5.79 14.21 4 12 4ZM12 14C9.33 14 4 15.34 4 18V20H20V18C20 15.34 14.67 14 12 14Z"
              fill="black"
            />
          </g>
          <defs>
            <clipPath id="clip0_4865_1210">
              <rect width="24" height="24" fill="white" />
            </clipPath>
          </defs>
        </svg>
      ),
    },
    {
      key: "2",
      label: <Link to="/settings">Account Settings</Link>,
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
        >
          <g clipPath="url(#clip0_4865_9057)">
            <path
              d="M19.1593 12.98C19.1993 12.66 19.2293 12.34 19.2293 12C19.2293 11.66 19.1993 11.34 19.1593 11.02L21.2693 9.37C21.4593 9.22 21.5093 8.95 21.3893 8.73L19.3893 5.27C19.2993 5.11 19.1293 5.02 18.9493 5.02C18.8893 5.02 18.8293 5.03 18.7793 5.05L16.2893 6.05C15.7693 5.65 15.2093 5.32 14.5993 5.07L14.2193 2.42C14.1893 2.18 13.9793 2 13.7293 2H9.72933C9.47933 2 9.26933 2.18 9.23933 2.42L8.85933 5.07C8.24933 5.32 7.68933 5.66 7.16933 6.05L4.67933 5.05C4.61933 5.03 4.55933 5.02 4.49933 5.02C4.32933 5.02 4.15933 5.11 4.06933 5.27L2.06933 8.73C1.93933 8.95 1.99933 9.22 2.18933 9.37L4.29933 11.02C4.25933 11.34 4.22933 11.67 4.22933 12C4.22933 12.33 4.25933 12.66 4.29933 12.98L2.18933 14.63C1.99933 14.78 1.94933 15.05 2.06933 15.27L4.06933 18.73C4.15933 18.89 4.32933 18.98 4.50933 18.98C4.56933 18.98 4.62933 18.97 4.67933 18.95L7.16933 17.95C7.68933 18.35 8.24933 18.68 8.85933 18.93L9.23933 21.58C9.26933 21.82 9.47933 22 9.72933 22H13.7293C13.9793 22 14.1893 21.82 14.2193 21.58L14.5993 18.93C15.2093 18.68 15.7693 18.34 16.2893 17.95L18.7793 18.95C18.8393 18.97 18.8993 18.98 18.9593 18.98C19.1293 18.98 19.2993 18.89 19.3893 18.73L21.3893 15.27C21.5093 15.05 21.4593 14.78 21.2693 14.63L19.1593 12.98ZM17.1793 11.27C17.2193 11.58 17.2293 11.79 17.2293 12C17.2293 12.21 17.2093 12.43 17.1793 12.73L17.0393 13.86L17.9293 14.56L19.0093 15.4L18.3093 16.61L17.0393 16.1L15.9993 15.68L15.0993 16.36C14.6693 16.68 14.2593 16.92 13.8493 17.09L12.7893 17.52L12.6293 18.65L12.4293 20H11.0293L10.8393 18.65L10.6793 17.52L9.61933 17.09C9.18933 16.91 8.78933 16.68 8.38933 16.38L7.47933 15.68L6.41933 16.11L5.14933 16.62L4.44933 15.41L5.52933 14.57L6.41933 13.87L6.27933 12.74C6.24933 12.43 6.22933 12.2 6.22933 12C6.22933 11.8 6.24933 11.57 6.27933 11.27L6.41933 10.14L5.52933 9.44L4.44933 8.6L5.14933 7.39L6.41933 7.9L7.45933 8.32L8.35933 7.64C8.78933 7.32 9.19933 7.08 9.60933 6.91L10.6693 6.48L10.8293 5.35L11.0293 4H12.4193L12.6093 5.35L12.7693 6.48L13.8293 6.91C14.2593 7.09 14.6593 7.32 15.0593 7.62L15.9693 8.32L17.0293 7.89L18.2993 7.38L18.9993 8.59L17.9293 9.44L17.0393 10.14L17.1793 11.27ZM11.7293 8C9.51933 8 7.72933 9.79 7.72933 12C7.72933 14.21 9.51933 16 11.7293 16C13.9393 16 15.7293 14.21 15.7293 12C15.7293 9.79 13.9393 8 11.7293 8ZM11.7293 14C10.6293 14 9.72933 13.1 9.72933 12C9.72933 10.9 10.6293 10 11.7293 10C12.8293 10 13.7293 10.9 13.7293 12C13.7293 13.1 12.8293 14 11.7293 14Z"
              fill="black"
            />
          </g>
          <defs>
            <clipPath id="clip0_4865_9057">
              <rect width="24" height="24" fill="white" />
            </clipPath>
          </defs>
        </svg>
      ),
    },
    {
      key: "3",
      label: <Link to="/notification-settings">Notification Settings</Link>,
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
        >
          <g clipPath="url(#clip0_4865_3241)">
            <path
              d="M12.0013 22C13.1013 22 14.0013 21.1 14.0013 20H10.0013C10.0013 21.1 10.9012 22 12.0013 22ZM18.0013 16V11C18.0013 7.93 16.3713 5.36 13.5013 4.68V4C13.5013 3.17 12.8313 2.5 12.0013 2.5C11.1713 2.5 10.5013 3.17 10.5013 4V4.68C7.64125 5.36 6.00125 7.92 6.00125 11V16L4.00125 18V19H20.0012V18L18.0013 16ZM16.0013 17H8.00125V11C8.00125 8.52 9.51125 6.5 12.0013 6.5C14.4913 6.5 16.0013 8.52 16.0013 11V17ZM7.58125 4.08L6.15125 2.65C3.75125 4.48 2.17125 7.3 2.03125 10.5H4.03125C4.18125 7.85 5.54125 5.53 7.58125 4.08ZM19.9712 10.5H21.9712C21.8212 7.3 20.2413 4.48 17.8513 2.65L16.4313 4.08C18.4513 5.53 19.8212 7.85 19.9712 10.5Z"
              fill="black"
            />
          </g>
          <defs>
            <clipPath id="clip0_4865_3241">
              <rect width="24" height="24" fill="white" />
            </clipPath>
          </defs>
        </svg>
      ),
    },
    {
      key: "4",
      label: (
        <div onClick={(e) => handleLogout(e)}>Logout</div>
        // <Link   onClick={(e) => handleLogout(e)}>
        //   Logout
        // </Link>
      ),
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
        >
          <g clipPath="url(#clip0_4865_3831)">
            <path
              d="M10.09 15.59L11.5 17L16.5 12L11.5 7L10.09 8.41L12.67 11H3V13H12.67L10.09 15.59ZM19 3H5C3.89 3 3 3.9 3 5V9H5V5H19V19H5V15H3V19C3 20.1 3.89 21 5 21H19C20.1 21 21 20.1 21 19V5C21 3.9 20.1 3 19 3Z"
              fill="black"
            />
          </g>
          <defs>
            <clipPath id="clip0_4865_3831">
              <rect width="24" height="24" fill="white" />
            </clipPath>
          </defs>
        </svg>
      ),
    },
  ];

  const menuItems = [
    {
      label: <NavLink to="/dashboard">Dashboard</NavLink>,
      key: "dashboard",
      icon: (
        <img src={dashboardsvg} alt="dashboardsvg"/>
      ),
    },
    {
      label: "Sms",
      key: "sms",
      icon: (
        <button>
          <MaterialIcon color="#7792B7" icon="attach_money" />
        </button>
      ),
      children: [
        {
          key: "request",
          label: (
            <NavLink className="ml-[1rem]" to="/sms-request-list">
             Request
            </NavLink>
          ),
        },
      ],
    },

    {
      label: <NavLink to="/tenants">step2</NavLink>,
      key: "tenants",
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
        >
          <path
            d="M12.3 12.22C12.8336 11.7581 13.2616 11.1869 13.5549 10.545C13.8482 9.90316 14 9.20571 14 8.5C14 7.17392 13.4732 5.90215 12.5355 4.96447C11.5979 4.02678 10.3261 3.5 9 3.5C7.67392 3.5 6.40215 4.02678 5.46447 4.96447C4.52678 5.90215 4 7.17392 4 8.5C3.99999 9.20571 4.1518 9.90316 4.44513 10.545C4.73845 11.1869 5.16642 11.7581 5.7 12.22C4.30014 12.8539 3.11247 13.8775 2.27898 15.1685C1.4455 16.4596 1.00147 17.9633 1 19.5C1 19.7652 1.10536 20.0196 1.29289 20.2071C1.48043 20.3946 1.73478 20.5 2 20.5C2.26522 20.5 2.51957 20.3946 2.70711 20.2071C2.89464 20.0196 3 19.7652 3 19.5C3 17.9087 3.63214 16.3826 4.75736 15.2574C5.88258 14.1321 7.4087 13.5 9 13.5C10.5913 13.5 12.1174 14.1321 13.2426 15.2574C14.3679 16.3826 15 17.9087 15 19.5C15 19.7652 15.1054 20.0196 15.2929 20.2071C15.4804 20.3946 15.7348 20.5 16 20.5C16.2652 20.5 16.5196 20.3946 16.7071 20.2071C16.8946 20.0196 17 19.7652 17 19.5C16.9985 17.9633 16.5545 16.4596 15.721 15.1685C14.8875 13.8775 13.6999 12.8539 12.3 12.22ZM9 11.5C8.40666 11.5 7.82664 11.3241 7.33329 10.9944C6.83994 10.6648 6.45542 10.1962 6.22836 9.64805C6.0013 9.09987 5.94189 8.49667 6.05764 7.91473C6.1734 7.33279 6.45912 6.79824 6.87868 6.37868C7.29824 5.95912 7.83279 5.6734 8.41473 5.55764C8.99667 5.44189 9.59987 5.5013 10.1481 5.72836C10.6962 5.95542 11.1648 6.33994 11.4944 6.83329C11.8241 7.32664 12 7.90666 12 8.5C12 9.29565 11.6839 10.0587 11.1213 10.6213C10.5587 11.1839 9.79565 11.5 9 11.5ZM18.74 11.82C19.38 11.0993 19.798 10.2091 19.9438 9.25634C20.0896 8.30362 19.9569 7.32907 19.5618 6.45C19.1666 5.57093 18.5258 4.8248 17.7165 4.30142C16.9071 3.77805 15.9638 3.49974 15 3.5C14.7348 3.5 14.4804 3.60536 14.2929 3.79289C14.1054 3.98043 14 4.23478 14 4.5C14 4.76522 14.1054 5.01957 14.2929 5.20711C14.4804 5.39464 14.7348 5.5 15 5.5C15.7956 5.5 16.5587 5.81607 17.1213 6.37868C17.6839 6.94129 18 7.70435 18 8.5C17.9986 9.02524 17.8593 9.5409 17.5961 9.99542C17.3328 10.4499 16.9549 10.8274 16.5 11.09C16.3517 11.1755 16.2279 11.2977 16.1404 11.4447C16.0528 11.5918 16.0045 11.7589 16 11.93C15.9958 12.0998 16.0349 12.2678 16.1137 12.4183C16.1924 12.5687 16.3081 12.6967 16.45 12.79L16.84 13.05L16.97 13.12C18.1754 13.6917 19.1923 14.596 19.901 15.7263C20.6096 16.8566 20.9805 18.1659 20.97 19.5C20.97 19.7652 21.0754 20.0196 21.2629 20.2071C21.4504 20.3946 21.7048 20.5 21.97 20.5C22.2352 20.5 22.4896 20.3946 22.6771 20.2071C22.8646 20.0196 22.97 19.7652 22.97 19.5C22.9782 17.9654 22.5938 16.4543 21.8535 15.1101C21.1131 13.7659 20.0413 12.6333 18.74 11.82Z"
            fill="#7792B7"
          />
        </svg>
      ),
    },
  ];

  const fullWidth = true;

  return (
    <>
      <MobileDrawer onClose={onClose} open={open} />
      <div className="w-full max-h-full h-[3.75rem]">
        {/* <div
          style={{
            boxShadow:
              "0px 1px 10px 0px rgba(0, 0, 0, 0.12), 0px 4px 5px 0px rgba(0, 0, 0, 0.14), 0px 2px 4px -1px rgba(0, 0, 0, 0.20)",
          }}
          className="w-full h-[3.75rem] bg-white flex items-center justify-between "
        >
          <div className="flex items-center">
            <button
              className="block lg:hidden"
              onClick={showDrawer}
              type="button"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                className="ml-[10px] object-contain max-w-none w-auto h-[57px]"
              >
                <path
                  d="M3 7H21C21.2652 7 21.5196 6.89464 21.7071 6.70711C21.8946 6.51957 22 6.26522 22 6C22 5.73478 21.8946 5.48043 21.7071 5.29289C21.5196 5.10536 21.2652 5 21 5H3C2.73478 5 2.48043 5.10536 2.29289 5.29289C2.10536 5.48043 2 5.73478 2 6C2 6.26522 2.10536 6.51957 2.29289 6.70711C2.48043 6.89464 2.73478 7 3 7ZM21 17H3C2.73478 17 2.48043 17.1054 2.29289 17.2929C2.10536 17.4804 2 17.7348 2 18C2 18.2652 2.10536 18.5196 2.29289 18.7071C2.48043 18.8946 2.73478 19 3 19H21C21.2652 19 21.5196 18.8946 21.7071 18.7071C21.8946 18.5196 22 18.2652 22 18C22 17.7348 21.8946 17.4804 21.7071 17.2929C21.5196 17.1054 21.2652 17 21 17ZM21 13H3C2.73478 13 2.48043 13.1054 2.29289 13.2929C2.10536 13.4804 2 13.7348 2 14C2 14.2652 2.10536 14.5196 2.29289 14.7071C2.48043 14.8946 2.73478 15 3 15H21C21.2652 15 21.5196 14.8946 21.7071 14.7071C21.8946 14.5196 22 14.2652 22 14C22 13.7348 21.8946 13.4804 21.7071 13.2929C21.5196 13.1054 21.2652 13 21 13ZM21 9H3C2.73478 9 2.48043 9.10536 2.29289 9.29289C2.10536 9.48043 2 9.73478 2 10C2 10.2652 2.10536 10.5196 2.29289 10.7071C2.48043 10.8946 2.73478 11 3 11H21C21.2652 11 21.5196 10.8946 21.7071 10.7071C21.8946 10.5196 22 10.2652 22 10C22 9.73478 21.8946 9.48043 21.7071 9.29289C21.5196 9.10536 21.2652 9 21 9Z"
                  fill="black"
                />
              </svg>
            </button>
            <img
              loading="lazy"
              src={emblem}
              className="ml-[1.19rem] object-contain max-w-none w-[3.25rem] h-[2.875rem]"
              alt="kenya Emplem"
            />
            <img
              loading="lazy"
              src={neaLogo}
              className="hidden lg:block ml-[1.06rem] object-contain w-[5.875rem] h-[2.0625rem]"
              alt="NEAIMIS Logo"
            />
          </div>

          <div className="flex items-center">
            <div className="mr-[2.94rem] rounded-full border border-darkBlue ml-[3rem]">
              <Dropdown
                overlayStyle={{
                  width: "250px",
                }}
                menu={{
                  items,
                }}
                placement="bottomLeft"
              >
                <img
                  loading="lazy"
                  className="w-[2.5rem] h-[2.5rem] object-cover rounded-full"
                  src={
                    user?.usrProfileImage
                      ? formatImgPath(user?.usrProfileImage)
                      : "https://kazi254.ke/myimages/OTP_IMAGES/ACCOUNT_OPENING/avatar.png"
                  }
                  alt="user-profile"
                />
              </Dropdown>
            </div>
          </div>
        </div> */}

        <div className="flex">
          <div
            className={`hidden lg:block xl:w-[18%] lg:w-[14.75rem] ml-0 h-[85vh] max-h-[100vh] 
          bg-white p-3 overflow-y-scroll`}
          >
            <div onClick={() => setnotOpen(false)} className="mt-4">
              <Menu
                style={{
                  height: !fullWidth ? "100vh" : "auto",
                  paddingTop: !fullWidth ? "20px" : "5px",
                }}
                defaultSelectedKeys={["1"]}
                defaultOpenKeys={["sub1"]}
                mode="inline"
                theme="light"
                inlineCollapsed={
                  pathname === "/profile" ||
                  pathname === "/cv-cover-letter/create/resume" ||
                  pathname === "/cv-cover-letter/edit/resume" ||
                  pathname === "/cv-cover-letter/edit/cover-letter" ||
                  pathname === "/cv-cover-letter/create/cover-letter" ||
                  pathname === "/job-vacancies"
                }
                items={menuItems}
                onClick={(item) => {}}
              />
            </div>
          </div>
          <div
            onClick={() => setnotOpen(false)}
            className=""
          >
            {children}
          </div>
        </div>
      </div>
    </>
  );
}
