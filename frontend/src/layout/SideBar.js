import React, { useEffect } from 'react';
import { useSelector } from 'react-redux';
import SideBarClosed from './SideBarClosed';
import SideBarOpen from './SideBarOpen';
import DashboardLayout from './DashboardLayout';

export default function SideBar() {
	const { sideMenuCollapsed } = useSelector((state) => state.global);
 

	useEffect(() => {}, [sideMenuCollapsed]);

	if (sideMenuCollapsed) return <SideBarClosed />;

	return <SideBarOpen />;
}
