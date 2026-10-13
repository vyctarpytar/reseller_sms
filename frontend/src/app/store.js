import { configureStore, combineReducers } from '@reduxjs/toolkit';
import { persistReducer, persistStore } from 'redux-persist';
import rawStorage from 'redux-persist/lib/storage';

// Vite 8 / rolldown's CJS→ESM interop hands back redux-persist's module `exports`
// object ({ __esModule, default }) instead of unwrapping the default export, so
// `storage.getItem` is undefined ("storage.getItem is not a function"). Unwrap it.
const storage = rawStorage?.getItem ? rawStorage : rawStorage?.default;
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
import smppReducer from '../features/smpp/smppSlice';
import { clearTenantScope } from '../custom_hooks/useTenantScope';

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
	smpp:smppReducer,
});

const persistedReducer = persistReducer(persistConfig, rootReducer);

// A session that ends, or is reset on the login page, must not hand its reseller /
// account scope to the next one. Done here, after the reducer has run, so no
// logout path can forget it (the scope notifies React, which a reducer must not).
const sessionEnd = ['auth/logout', 'auth/clearAuthObj'];
const tenantScopeGuard = () => (next) => (action) => {
	const result = next(action);
	if (sessionEnd.includes(action?.type)) clearTenantScope();
	return result;
};

export const store = configureStore({
	reducer: persistedReducer,
	middleware: (getDefaultMiddleware) =>
		getDefaultMiddleware({
			serializableCheck: false,
		}).concat(tenantScopeGuard),
});

export const persistor = persistStore(store);
