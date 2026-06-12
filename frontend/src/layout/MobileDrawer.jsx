import React, { useEffect, useState } from "react";
import { Drawer, Menu } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { Link, NavLink, useNavigate } from "react-router-dom";
import MaterialIcon from "material-icons-react";
import resellerSvg from "../assets/svg/resellerSvg.svg";
import usersSvg from "../assets/svg/usersSvg.svg";
import airtimeSvg from "../assets/svg/airtimeSvg.svg";
import accountSvg from "../assets/svg/accountSvg.svg";
import requestSvg from "../assets/svg/requestSvg.svg";
import gridSvg from "../assets/svg/grid.svg";
import smsSvg from "../assets/svg/smsSvg.svg";
import creditSvg from "../assets/svg/creditSvg.svg";
import adminAccountSvg from "../assets/svg/adminAccountSvg.svg";
import externalSvg from "../assets/svg/externalSvg.svg";
import { fetchMenu } from "../features/menu/menuSlice";
import passwordSvg from "../assets/svg/passwordSvg.svg";
import reportSvg from "../assets/svg/svg60.svg";

const icons = {
  gridSvg,
  smsSvg,
  resellerSvg,
  usersSvg,
  airtimeSvg,
  accountSvg,
  requestSvg,
  passwordSvg,
  creditSvg,
  adminAccountSvg,
  externalSvg,
  reportSvg,
};
function getItem(label, key, icon, children, type) {
  return {
    key,
    icon,
    children,
    label,
    type,
  };
}
const MobileDrawer = ({ open, onClose }) => {
  const { user } = useSelector((state) => state.auth);
  const { menuData } = useSelector((state) => state.menu);
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const generateMenuItems = (menuData) => {
    if (user?.changePassword === true || user?.changePassword === null) {
      return [];
    }
    const mapItems = (items, isChild) => {
      return items?.map((item) => {
        const iconSrc = icons[item?.mnIcons] || gridSvg;
        return getItem(
          item?.mnName.charAt(0).toUpperCase() + item?.mnName.slice(1),
          item?.mnLink,
          !isChild ? <img src={iconSrc} alt={item?.mnIcons} /> : null,
          item?.children && item?.children?.length > 0
            ? mapItems(item?.children, true)
            : null
        );
      });
    };
    if (!menuData || menuData?.length === 0) {
      return [];
    }

    const initialMenuData = menuData?.filter(
      (item) =>
        item?.mnName === "Resellers" ||
        item?.mnName === "dashboard" ||
        item?.mnName === "Account" ||
        item?.mnName === "Users" ||
        item?.mnName === "Credits Statement"
    );

    // const fullMenuData = resellerId ? menuData : initialMenuData;
    // return user?.layer === "TOP" || user?.layer === "ACCOUNT"? mapItems(menuData) : mapItems(fullMenuData);
    return mapItems(menuData);
  };
  const items = [
    ...generateMenuItems(menuData),

    {
      type: "divider",
    },
  ];

  const handleNavigation = async (e) => {
    await navigate(e.key);
    await onClose();
  };

  const menuItems = [
    {
      label: <NavLink to="/dashboard">Dashboard</NavLink>,
      key: "dashboard",
      icon: (
        <svg
          width="25"
          height="24"
          viewBox="0 0 25 24"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          <g id="grid / 24 / Outline">
            <path
              id="Vector"
              d="M10.2656 13H3.26562C3.00041 13 2.74605 13.1054 2.55852 13.2929C2.37098 13.4804 2.26563 13.7348 2.26562 14V21C2.26563 21.2652 2.37098 21.5196 2.55852 21.7071C2.74605 21.8946 3.00041 22 3.26562 22H10.2656C10.5308 22 10.7852 21.8946 10.9727 21.7071C11.1603 21.5196 11.2656 21.2652 11.2656 21V14C11.2656 13.7348 11.1603 13.4804 10.9727 13.2929C10.7852 13.1054 10.5308 13 10.2656 13ZM9.26562 20H4.26562V15H9.26562V20ZM21.2656 2H14.2656C14.0004 2 13.7461 2.10536 13.5585 2.29289C13.371 2.48043 13.2656 2.73478 13.2656 3V10C13.2656 10.2652 13.371 10.5196 13.5585 10.7071C13.7461 10.8946 14.0004 11 14.2656 11H21.2656C21.5308 11 21.7852 10.8946 21.9727 10.7071C22.1603 10.5196 22.2656 10.2652 22.2656 10V3C22.2656 2.73478 22.1603 2.48043 21.9727 2.29289C21.7852 2.10536 21.5308 2 21.2656 2ZM20.2656 9H15.2656V4H20.2656V9ZM21.2656 13H14.2656C14.0004 13 13.7461 13.1054 13.5585 13.2929C13.371 13.4804 13.2656 13.7348 13.2656 14V21C13.2656 21.2652 13.371 21.5196 13.5585 21.7071C13.7461 21.8946 14.0004 22 14.2656 22H21.2656C21.5308 22 21.7852 21.8946 21.9727 21.7071C22.1603 21.5196 22.2656 21.2652 22.2656 21V14C22.2656 13.7348 22.1603 13.4804 21.9727 13.2929C21.7852 13.1054 21.5308 13 21.2656 13ZM20.2656 20H15.2656V15H20.2656V20ZM10.2656 2H3.26562C3.00041 2 2.74605 2.10536 2.55852 2.29289C2.37098 2.48043 2.26563 2.73478 2.26562 3V10C2.26563 10.2652 2.37098 10.5196 2.55852 10.7071C2.74605 10.8946 3.00041 11 3.26562 11H10.2656C10.5308 11 10.7852 10.8946 10.9727 10.7071C11.1603 10.5196 11.2656 10.2652 11.2656 10V3C11.2656 2.73478 11.1603 2.48043 10.9727 2.29289C10.7852 2.10536 10.5308 2 10.2656 2ZM9.26562 9H4.26562V4H9.26562V9Z"
              fill="#7792B7"
            />
          </g>
        </svg>
      ),
    },
    {
      label: "step1",
      key: "financials",
      icon: (
        <button>
          <MaterialIcon color="#7792B7" icon="attach_money" />
        </button>
      ),
      children: [
        {
          key: "Invoices",
          label: (
            <NavLink className="ml-[1rem]" to="/invoices">
              step 1_b
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

  // useEffect(() => {
  //   dispatch(fetchMenu());
  // }, []);

  return (
    <>
      <Drawer
        className="mobile-drawer "
        title="Basic Drawer"
        placement={"left"}
        closable={true}
        onClose={onClose}
        open={open}
        key={"1"}
      >
        <div className="w-[100%] h-[95vh]  max-h-[100vh] bg-white p-0 cv-scroll overflow-y-scroll">
          <div className="flex justify-end p-2 mb-3">
            <button onClick={() => onClose()} type="button">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
              >
                <path
                  d="M13.408 11.9963L19.708 5.70628C19.8963 5.51798 20.0021 5.26258 20.0021 4.99628C20.0021 4.72998 19.8963 4.47458 19.708 4.28628C19.5197 4.09798 19.2643 3.99219 18.998 3.99219C18.7317 3.99219 18.4763 4.09798 18.288 4.28628L11.998 10.5863L5.70799 4.28628C5.51968 4.09798 5.26429 3.99219 4.99799 3.99219C4.73168 3.99219 4.47629 4.09798 4.28799 4.28628C4.09968 4.47458 3.99389 4.72998 3.99389 4.99628C3.99389 5.26258 4.09968 5.51798 4.28799 5.70628L10.588 11.9963L4.28799 18.2863C4.19426 18.3792 4.11986 18.4898 4.06909 18.6117C4.01833 18.7336 3.99219 18.8643 3.99219 18.9963C3.99219 19.1283 4.01833 19.259 4.06909 19.3809C4.11986 19.5027 4.19426 19.6133 4.28799 19.7063C4.38095 19.8 4.49155 19.8744 4.61341 19.9252C4.73527 19.9759 4.86597 20.0021 4.99799 20.0021C5.13 20.0021 5.2607 19.9759 5.38256 19.9252C5.50442 19.8744 5.61502 19.8 5.70799 19.7063L11.998 13.4063L18.288 19.7063C18.3809 19.8 18.4915 19.8744 18.6134 19.9252C18.7353 19.9759 18.866 20.0021 18.998 20.0021C19.13 20.0021 19.2607 19.9759 19.3826 19.9252C19.5044 19.8744 19.615 19.8 19.708 19.7063C19.8017 19.6133 19.8761 19.5027 19.9269 19.3809C19.9776 19.259 20.0038 19.1283 20.0038 18.9963C20.0038 18.8643 19.9776 18.7336 19.9269 18.6117C19.8761 18.4898 19.8017 18.3792 19.708 18.2863L13.408 11.9963Z"
                  fill="black"
                />
              </svg>
            </button>
          </div>

          <div className="mt-4">
            {items?.length > 0 ? (
              <Menu
                onClick={handleNavigation}
                style={{
                  height: "auto",
                  paddingTop: "5px",
                }}
                defaultSelectedKeys={["1"]}
                defaultOpenKeys={["sub1"]}
                mode="inline"
                theme="light"
                inlineCollapsed={false}
                items={items}
              />
            ) : (
              <p>Loading...</p>
            )}
          </div>
        </div>
      </Drawer>
    </>
  );
};
export default MobileDrawer;
