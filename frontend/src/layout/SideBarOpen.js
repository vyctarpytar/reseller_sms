import React, { useEffect, useState } from "react"; 
import { Avatar, Badge, Dropdown, Menu } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { useLocation, useNavigate } from "react-router-dom";
import gridSvg from "../assets/svg/grid.svg";
import smsSvg from "../assets/svg/smsSvg.svg";
import svg23 from "../assets/svg/svg23.svg";
import resellerSvg from "../assets/svg/resellerSvg.svg";
import usersSvg from "../assets/svg/usersSvg.svg";
import airtimeSvg from "../assets/svg/airtimeSvg.svg";
import accountSvg from "../assets/svg/accountSvg.svg";
import requestSvg from "../assets/svg/requestSvg.svg";
import { fetchMenu } from "../features/menu/menuSlice";
import passwordSvg from "../assets/svg/passwordSvg.svg";
import creditSvg from "../assets/svg/creditSvg.svg";
import adminAccountSvg from "../assets/svg/adminAccountSvg.svg";
import externalSvg from "../assets/svg/externalSvg.svg";
import withdrawalSvg from "../assets/svg/withdrawalSvg.svg";
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
  withdrawalSvg,
  reportSvg
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

export default function SideBarOpen() {
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const { sideMenuCollapsed,resellerId } = useSelector((state) => state.global);
  const { menuData } = useSelector((state) => state.menu);
  const { user } = useSelector((state) => state.auth);

 
 
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

    
  const initialMenuData = menuData?.filter(item => item?.mnName === "Resellers" || item?.mnName === "dashboard" || 
    item?.mnName === "Account" || item?.mnName === "Users" || item?.mnName === "Credits Statement" );
 
  const fullMenuData = resellerId ? menuData : initialMenuData;
  // return user?.layer === "TOP" || user?.layer === "ACCOUNT"? mapItems(menuData) : mapItems(fullMenuData);
    return mapItems(menuData);
  };
  const items = [
    ...generateMenuItems(menuData,resellerId),
   
    {
      type: "divider",
    }, 
   
  ];

  const location = useLocation();
  const currentPath = location.pathname;

  const [lastSelectedKey, setLastSelectedKey] = useState(null);

  const handleNavigation = async (e) => {
    setLastSelectedKey(e.key);
    await navigate(e.key);
  };


  const selectedKeyFromPath = currentPath?.replace("/", "");
  const validKeys = menuData?.map((item) => item?.mnLink);
  const selectedKey = validKeys?.includes(selectedKeyFromPath)
    ? selectedKeyFromPath
    : lastSelectedKey;

  const defaultOpenKeys = items
  .filter(item => item?.mnName === "Billing" || item?.children)
  .map(item => item?.key);

  const defaultSelectedKeys = items
  .filter(item => item?.mnName === "Dashboard")
  .map(item => item?.key);


  const [openKeys, setOpenKeys] = useState([]);
  const handleOpenChange = (keys) => {
    const latestOpenKey = keys.length ? keys[keys.length - 1] : "";
    setOpenKeys(latestOpenKey ? [latestOpenKey] : []);
  };

  useEffect(() => {}, [sideMenuCollapsed]);

  // useEffect(() => {
  //   dispatch(fetchMenu());
  // }, []);
 
  return (
    
      <div
        style={{
          borderInlineEnd: "1px solid rgba(5, 5, 5, 0.06)",
          position: "sticky",
          top: 0,
        }}
        className="lg:flex hidden flex-col min-h-[90vh] h-full overflow-y-scroll max-w-[256px] w-full bg-[#f4f5f7]  mr-5 custom-scrollbar"
      >
        {items?.length > 0 ? (
          <Menu
            onClick={handleNavigation}
            style={{
              width: 256,
            }}
            className="overflow-y-scroll side-bar-scroll"
            selectedKeys={selectedKey ? [selectedKey] : []}
            // defaultSelectedKeys={defaultSelectedKeys}
            defaultOpenKeys={defaultOpenKeys}
            mode="inline"
            items={items}
            theme="dark"
            inlineCollapsed={sideMenuCollapsed}
            openKeys={openKeys}
            onOpenChange={handleOpenChange}
          />
        ) : (
          <p>Loading...</p>
        )}
      </div>
    
  );
}
