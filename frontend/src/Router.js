import { createHashRouter } from 'react-router-dom';
import Login from './pages/auth/Login';
import Dashboard from './pages/dashboard/Dashboard';
import RequestList from './pages/sms/RequestList';
import { ProtectedRoute } from './ProtectedRoute';
import Root from './Root';
import UsersList from './pages/users/UsersList';
import ProductRequestList from './pages/Request/ProductRequestList';
import ResellersList from './pages/reselller/ResellersList';
import ProductRequestView from './pages/Request/ProductRequestView';
import CodesRequestList from './pages/reseller-codes/CodesRequestList';
import ResellerCodesView from './pages/reseller-codes/ResellerCodesView';
import Billing from './pages/billing/Billing';
import PaymentMethod from './pages/billing/payment-method/PaymentMethod';
import AccountsList from './pages/reseller-accounts/AccountsList';
import CreditList from './pages/credit/CreditList';
import OutboxList from './pages/outbox/OutboxList';
import GroupSelection from './pages/outbox/group/GroupSelection';
import Group from './pages/outbox/group/Group';
import ContactFolderView from './pages/outbox/group/ContactFolderView';
import ContactsListView from './pages/outbox/group/ContactsListView';
import ContactUploadList from './pages/outbox/group/ContactUploadList';  
import ContactAdd from './pages/outbox/group/ContactAdd';
import GroupIndividualList from './pages/outbox/individual/GroupIndividualList';
import ExcelSms from './pages/excel-sms/ExcelSms';
import SentSmsList from './pages/sent-sms/SentSmsList';
import PasswordManagement from './pages/password-management/PasswordManagement';
import VerifyForgotEmail from './pages/auth/forgot-password/VerifyForgotEmail';
import NewPassword from './pages/auth/forgot-password/NewPassword';
import ForgotPassword from './pages/auth/forgot-password/ForgotPassword';
import DashboardAccount from './pages/dashboard-account/DashboardAccount';
import MessageTemplate from './pages/mesage-template/MessageTemplate';
import PasswordResetSuccess from './pages/auth/forgot-password/PasswordResetSuccess';
import Invoices from './pages/invoice/Invoices';
import RolesErrorPage from './components/RolesErrorPage';
import MainProfile from './pages/profile/MainProfile';
import AccountSettings from './pages/profile/AccountSettings';
import DashboardReseller from './pages/dashboard-reseller/DashboardReseller';
import AccountAdminList from './pages/admin-portal/AccountAdminList';
import AccountAdminView from './pages/admin-portal/AccountAdminView'; 
import DashboardMain from './pages/dashboard-main/DashboardMain';
import SmsTemplateList from './pages/template/SmsTemplateList';
import ApproveSms from './pages/admin-sms-approval/ApproveSms';
import SenderList from './pages/sender-id/SenderList';
import DynamicTable from './pages/outbox/individual/DynamicTable';
import Report from './pages/reports/Report';
import ContactEdit from './pages/outbox/group/ContactEdit'; 
import DailySms from './pages/reports/status-report/DailySms';
import StatusSummary from './pages/reports/status-report/StatusSummary';
import SandBox from './pages/sand-box/SandBox';
import WithdrawalList from './pages/withdrawal/WithdrawalList';
import WithdrawalPage from './pages/withdrawal/WithdrawalPage';
import ScheduledSmsList from './pages/scheduled-sms/ScheduledSmsList';
import QuarterlyReport from './pages/reports/quarterly-report/QuarterlyReport';
 
export const router = createHashRouter([
	{
		path: '/',
		element: <Root />,
		children: [
			{
				path: '/',
				element: <Login />,
				index: true,
			},
			{
				path: '/login',
				element: <Login />,
			},

			{
				path:'/forgot-password',
				element: <ForgotPassword />,
			},
			{
				path:'/verify-forgot-email',
				element: <VerifyForgotEmail />,
			},
			{
				path:'/new-password',
				element: <NewPassword />,
			},
			{
				path:'/password-reset-success',
				element: <PasswordResetSuccess />,
			},
		  
			  
			{
				element: <ProtectedRoute role={"EXECUTIVE"} />,
				children: [
					
					{
						path: '/dashboard-main',
						element: <DashboardMain />,
					},
					{
						path: '/dashboard',
						element: <Dashboard />,
					},
					{
						path: '/dashboard-account',
						element: <DashboardAccount />,
					},
					{
						path: '/dashboard-reseller',
						element: <DashboardReseller />,
					}, 
					
                    {
						path: '/sms-request-list',
						element: <RequestList />,
					},
					{
						path: '/users',
						element: <UsersList />,
					},
					{
						path: '/product-request-list',
						element: <ProductRequestList />,
					},
					{
						path: '/resellers-list',
						element: <ResellersList />,
					},
					{
						path: '/product-request-view/:id',
						element: <ProductRequestView />,
					},
					{
						path: '/code-request-list',
						element: <CodesRequestList />,
					},
					{
						path: '/reseller-code-view/:id',
						element: <ResellerCodesView />,
					},
					{
						path: '/payment-method',
						element: <PaymentMethod />,
					},
					{
						path: '/account-list',
						element: <AccountsList />,
					},
					{
						path: '/credit',
						element: <CreditList />,
					},
					{
						path: '/outbox',
						element: <OutboxList />,
					},
					{
						path: '/group-selection',
						element: <GroupSelection />,
					},
					{
						path: '/group',
						element: <Group />,
					},
					{
						path:'/contacts/folder/:name',
						element: <ContactFolderView />,
					},
					{
						path:'/contacts/folders/list/:name',
						element: <ContactsListView />,
					},
					{
						path:'/contacts/add/file-upload',
						element: <ContactUploadList />,
					},
					{
						path:'/group-individual-list',
						element: <GroupIndividualList />,
					},
					{
						path:'/contacts/add',
						element: <ContactAdd />,
					},
					{
						path:'/excel-sms',
						element: <ExcelSms />,
					},
					{
						path:'/sent-sms',
						element: <SentSmsList />,
					},
					{
						path:'/password-management',
						element: <PasswordManagement />,
					},
					{
						path:'/message-template',
						element: <MessageTemplate />,
					}, 
					{
						path:'/invoices',
						element: <Invoices />,
					}, 
					{
						path:'/roles-error-page',
						element: <RolesErrorPage />,
					}, 
					{
						path:'/profile',
						element: <MainProfile />,
					},
					{
						path:'/account-settings',
						element: <AccountSettings />,
					}, 
					{
						path:'/account-admin',
						element: <AccountAdminList />,
					}, 
					{
						path:'/account-admin-view/:id',
						element: <AccountAdminView />,
					}, 
					{
						path:'/billing',
						element: <Billing />,
					}, 
					{
						path:'/templates',
						element: <SmsTemplateList />,
					}, 
					{
						path:'/approve-sms',
						element: <ApproveSms />,
					}, 
					{
						path:'/sender-id-list',
						element: <SenderList />,
					}, 
					{
						path:'/dynamic-table',
						element: <DynamicTable />,
					}, 
					{
						path:'/report',
						element: <Report />,
					},
					{
						path:'/contact-edit/:id',
						element: <ContactEdit />,
					},
					{
						path:'/status-analysis',
						element: <StatusSummary />,
					},
					{
						path:'/date-analysis',
						element: <DailySms />,
					},
					{
						path:'/quartely-Report',
						element: <QuarterlyReport />,
					},
					
					{
						path:'/dev-sandbox',
						element: <SandBox />,
					},
					{
						path:'/withdrawal',
						element: <WithdrawalList />,
					},
					{
						path:'/withdrawal-page',
						element: <WithdrawalPage />,
					},

					{
						path:'/scheduled-sms',
						element: <ScheduledSmsList />,
					},

					
					
					
					
					
					
				],
			},
			{
				path: '*',
				element: <p>404 - Error: Page not found</p>,
			},
		],
	},
]);
