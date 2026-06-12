import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import './texts.css';
import './antd.css';
import App from './App';
import reportWebVitals from './reportWebVitals';
import { BrowserRouter } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { Provider } from 'react-redux';
import { persistor, store } from './app/store';
import { PersistGate } from 'redux-persist/integration/react';
import 'react-phone-input-2/lib/style.css';
import { ConfigProvider } from 'antd';
import { BRAND, BRAND_ACCENT } from './brand';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
	<Provider store={store}>
		<PersistGate loading={null} persistor={persistor}>
			<ConfigProvider
				theme={{
					token: {
						colorPrimary: BRAND,
						colorLink: BRAND,
						colorLinkHover: BRAND_ACCENT,
						colorInfo: BRAND,
						borderRadius: 10,
						fontFamily: "'DM Sans', sans-serif",
						fontSize: 14,
						controlHeight: 40,
						colorBgLayout: '#fafaf9',
						colorBorder: 'rgba(0,0,0,0.10)',
						colorBorderSecondary: 'rgba(0,0,0,0.06)',
						colorTextHeading: '#1f2937',
						boxShadow: '0 4px 6px -1px rgba(0,0,0,.08), 0 2px 4px -1px rgba(0,0,0,.05)',
						boxShadowSecondary: '0 10px 25px -5px rgba(0,0,0,.10), 0 8px 10px -6px rgba(0,0,0,.04)',
					},
					components: {
						Button: { fontWeight: 600, primaryShadow: 'none', controlHeight: 40, paddingContentHorizontal: 20 },
						Table: {
							headerBg: '#faf8f6',
							headerColor: '#6b5847',
							headerSplitColor: 'transparent',
							borderColor: 'rgba(0,0,0,0.06)',
							rowHoverBg: '#faf7f4',
							cellPaddingBlock: 14,
							headerBorderRadius: 12,
						},
						Modal: { borderRadiusLG: 16, paddingContentHorizontalLG: 28, titleFontSize: 18 },
						Card: { borderRadiusLG: 14, paddingLG: 24 },
						Input: { borderRadius: 10, controlHeight: 40, activeShadow: '0 0 0 3px rgba(105,71,46,0.12)' },
						Select: { borderRadius: 10, controlHeight: 40 },
						DatePicker: { borderRadius: 10, controlHeight: 40 },
						Tabs: { titleFontSize: 14, inkBarColor: BRAND_ACCENT, itemSelectedColor: BRAND, itemHoverColor: BRAND_ACCENT },
						Menu: { itemSelectedBg: 'rgba(105,71,46,0.08)', itemSelectedColor: BRAND, itemBorderRadius: 10, itemHeight: 44 },
						Pagination: { borderRadius: 8 },
						Tag: { borderRadiusSM: 6 },
					},
				}}
			>
				<App />
				<Toaster
					position='top-center'
					reverseOrder={false}
					toastOptions={{
						duration: 5000,
					}}
				/>
			</ConfigProvider>
		</PersistGate>
	</Provider>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
