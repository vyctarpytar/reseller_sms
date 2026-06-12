import { configureStore, combineReducers } from '@reduxjs/toolkit';
import { persistReducer, persistStore } from 'redux-persist';
import storage from 'redux-persist/lib/storage';
import authReducer from '../features/auth/authSlice';
import globalReducer from '../features/global/globalSlice'; 
import saveReducer from '../features/save/saveSlice';
import smsReducer from '../features/sms-request/smsRequestSlice';
import menuReducer from '../features/menu/menuSlice';
import productRequestReducer from '../features/product-request/productRequestSlice';
import resellerReducer from '../features/reseller/resellerSlice';
import resellerCodesReducer from '../features/reseller-codes/resellerCodesSlice';
import resellerAccountReducer from '../features/reseller-account/resellerAccountSlice';
import creditReducer from '../features/credit/creditSlice';
import folderReducer from '../features/folder/folderSlice';
import contactReducer from '../features/contact/ContactSlice';
import filterReducer from '../features/filter/filterSlice';
import dashReducer from '../features/dashboard/dashboardSlice';
import invoiceReducer from '../features/invoice/invoiceSlice';
import billingReducer from '../features/billing/billingSlice';
import templateReducer from '../features/sms-template/smsTemplateSlice';
import sandBoxReducer from '../features/sandbox/sandboxSlice';

const persistConfig = {
	key: 'root',
	storage,
};

const rootReducer = combineReducers({
	auth: authReducer,
	global: globalReducer, 
	save: saveReducer,
	sms:smsReducer,
	menu:menuReducer,
	productRequest:productRequestReducer,
	reseller:resellerReducer,
	resellerCodes:resellerCodesReducer,
	resellerAccount:resellerAccountReducer,
	credit:creditReducer,
	folder:folderReducer,
	contact:contactReducer,
	filterSms:filterReducer,
	dash:dashReducer,
	inv:invoiceReducer,
	billing:billingReducer,
	template:templateReducer,
	sand:sandBoxReducer,
});

const persistedReducer = persistReducer(persistConfig, rootReducer);

export const store = configureStore({
	reducer: persistedReducer,
	middleware: (getDefaultMiddleware) =>
		getDefaultMiddleware({
			serializableCheck: false,
		}),
});

export const persistor = persistStore(store);
