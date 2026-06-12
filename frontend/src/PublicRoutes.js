import { Navigate } from 'react-router-dom';
import Login from './pages/auth/Login';
import ForgotPassword from './pages/auth/ForgotPassword';
import Register from './pages/auth/Register';
import OTPCode from './components/OTPCode';
import NewPassword from './pages/auth/NewPassword';
import Layout from './layout/Layout';
import { Puff } from 'react-loader-spinner';

export const PublicRoutes = () => {
	return {
		path: '/',
		element: <Layout />,
		children: [
			{ path: 'account/login/*', element: <Login /> },
			{ path: 'account/forgot-password/*', element: <ForgotPassword /> },
			{ path: 'account/register/*', element: <Register /> },
			{ path: 'account/verify-otp/*', element: <OTPCode /> },
			{ path: 'account/new-password/*', element: <NewPassword /> },
			{ path: '*', element: <Navigate to='/account/login' replace /> },
		],
	};
};
