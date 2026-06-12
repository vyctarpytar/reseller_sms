import { lazy } from "react";
import { Navigate } from "react-router-dom";
import Layout from "./layout/Layout";
import pMinDelay from "p-min-delay";

// const DashBoard = lazy(() => import("./pages/dashboard/Dashboard"));
// const Settings = lazy(() => import("./pages/settings/Settings"));
const DashBoard = lazy(() => pMinDelay(import("./pages/dashboard/Dashboard"), 5000));
const Settings = lazy(() => pMinDelay(import("./pages/settings/Settings"), 5000));


export const PrivateRoutes = () => {
  return {
    element: <Layout />,
    children: [
      { path: "/", element: <DashBoard /> },
      { path: "/settings", element: <Settings /> },
      { path: "*", element: <Navigate to="/" replace /> },
    ],
  };
};
