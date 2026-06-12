import React, { useEffect } from "react";
import {
  AppstoreOutlined,
  ContainerOutlined,
  DesktopOutlined,
  MailOutlined,
  PieChartOutlined,
} from "@ant-design/icons";
import { Avatar, Badge, Dropdown, Menu } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { fetchMenu } from "../features/menu/menuSlice";
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
import passwordSvg from "../assets/svg/passwordSvg.svg";
import withdrawalSvg from "../assets/svg/withdrawalSvg.svg";
import reportSvg from "../assets/svg/svg60.svg";

import { useNavigate } from "react-router-dom";

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
 
export default function SideBarClosed() {
  const { sideMenuCollapsed } = useSelector((state) => state.global);
  const { menuData } = useSelector((state) => state.menu);
  const { user } = useSelector((state) => state.auth);
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const generateMenuItems = (menuData) => {
    if (user?.changePassword === true || user?.changePassword === null) {
      return [];
    }
    const mapItems = (items, isChild) => {
      return items?.map((item) => {
        const iconSrc = icons[item?.mnIcons] || gridSvg;
        const rawLabel =
          item?.mnName.charAt(0).toUpperCase() + item?.mnName.slice(1);
        const label =
          rawLabel?.toLowerCase() === "wallet" ? "Mpesa Wallet" : rawLabel;
        return getItem(
          label,
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
    await navigate(e?.key);
  };
  useEffect(() => {}, [sideMenuCollapsed]);

  // useEffect(() => {
  //   dispatch(fetchMenu());
  // }, []);
  return (
    <>
      <div
        style={{
          borderInlineEnd: "1px solid rgba(5, 5, 5, 0.06)",
        }}
        className="flex flex-col h-full bg-[#f4f5f7]"
      >
        <Menu
          onClick={handleNavigation}
          defaultSelectedKeys={["dash", "/dashboard"]}
          defaultOpenKeys={[]}
          mode="inline"
          theme="light"
          inlineCollapsed={sideMenuCollapsed}
          // inlineCollapsed={true}
          items={items}
        />
      </div>
    </>
  );
}
