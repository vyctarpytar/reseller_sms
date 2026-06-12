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

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
	<Provider store={store}>
		<PersistGate loading={null} persistor={persistor}>
		 
				<App />
				<Toaster
					position='top-center'
					reverseOrder={false}
					toastOptions={{
						duration: 5000,
					}}
				/>
				 
		</PersistGate>
	</Provider>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
