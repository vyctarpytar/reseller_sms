# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Multi-tenant **reseller SMS platform** front-end (React 18 + Create React App). The `package.json` name is `ezambia`; the product is white-labeled per reseller (logos, side images, and theming swap based on the subdomain — see `getSubdomain()` and the `synctel`/`futuresoft`/`sync` asset sets in `src/assets/img/`). The backend is a Java/SMS gateway exposing `/api/v1` and `/api/v2` endpoints; this repo is UI only.

## Commands

```bash
npm start          # dev server on http://localhost:3000 (CRA)
npm run build      # production build to /build
npm test           # CRA/Jest watch mode (Testing Library)
npm test -- --watchAll=false src/App.test.js   # run a single test file, no watch
```

There is no separate lint script — ESLint runs through `react-scripts` (config `react-app`, `react-app/jest` in `package.json`) and surfaces in the dev server / build output.

### Environment

`.env` (gitignored, already present locally) holds:
- `REACT_APP_API_BASE_URL` — backend base URL; read as `process.env.REACT_APP_API_BASE_URL` in every slice. **This is the source of truth for the API host**, not `src/config/constant.js` (whose `BASE_URL` constant is stale/unused by the axios layer).
- `REACT_APP_GOOGLE`, `NODE_ENV`.

## Architecture

### Routing (HashRouter)
`src/App.js` → `RouterProvider` with the router in **`src/Router.js`** (`createHashRouter`, so URLs use `/#/...`). This is the live router. `src/PrivateRoutes.js` and `src/PublicRoutes.js` are **legacy/unused** (they reference pages like `Settings`/`Register` that don't exist) — do not edit them expecting an effect.

- `Root.js` is the router root: renders `<Header />` (when logged in) + `<Outlet />`, and on mount/token change validates the JWT (`jwtDecode`), logs out + redirects to `/login` if missing/expired, and dispatches `fetchMenu()` whenever `user`/`selectedOrg`/`selectedAccount` change.
- `ProtectedRoute.js` wraps all authenticated routes (`<ProtectedRoute role="EXECUTIVE" />`), renders `<SideBar /> + <Outlet />`, re-checks token expiry, and force-redirects to `/account-settings` when `user.changePassword` is true/null.
- Auth pages (`/login`, `/forgot-password`, etc.) live outside `ProtectedRoute`.
- Add a new page by: creating it under `src/pages/<feature>/`, importing it in `Router.js`, and adding a `{ path, element }` inside the `ProtectedRoute` children array.

### State — Redux Toolkit + redux-persist
Store: `src/app/store.js`. The **entire root reducer is persisted to `localStorage`** (`persistConfig` key `root`), and `serializableCheck` is disabled. Each domain is a slice under `src/features/<domain>/<name>Slice.js` and must be registered in the `combineReducers` map in `store.js`.

Slice convention (see `authSlice`, `saveSlice`, `menuSlice`):
- Data fetching/mutation = `createAsyncThunk` calling **`axiosInstance`** (the configured client), with `pending/fulfilled/rejected` handled in `extraReducers` toggling a `loading`/`saving` boolean and storing results.
- A very common generic thunk pattern: the caller passes `{ url, ...payload }`, the thunk does `let saveUrl = data.url; delete data.url;` then `axiosInstance.post(\`${url}/${saveUrl}\`, data)`. So pages choose the endpoint at dispatch time, e.g. `dispatch(save({ url: "api/v2/...", ...body }))`. The shared CRUD thunks (`save`, `saveArray`, `deleteRequest`, `fetchSavedSms`, `fetchSenderIds`, `fetchTemplates`, `fetchScheduledSms`, file uploads) live in **`src/features/save/saveSlice.js`** — reuse them rather than adding per-page thunks.
- Thunks treat `response.data.success === false` as an error via `rejectWithValue(response.data)`. API responses are typically unwrapped as `res.data?.data?.result` (lists also carry `total` for pagination counts).

### HTTP layer — `src/instance.js`
`axiosInstance` is the shared axios client. Key behavior to preserve:
- **Request interceptor injects multi-tenancy params on every call**: `reseller_id` (from `localStorage.selectedOrg`) and `account_id` (from `localStorage.selectedAccount`). Switching tenant = updating these localStorage keys (done in `HeaderCrumb.jsx` / dashboards), which is why `Root` re-fetches the menu when they change.
- **Response interceptor auto-logs-out on 401/403.**
- The bearer token is set separately in `ProtectedRoute.js` via `axiosInstance.defaults.headers.common["Authorization"]`.
- Some thunks (login, password reset, `save` in `authSlice`) call raw `axios` instead of `axiosInstance` — intentional for unauthenticated/pre-token calls.

### Auth flow
Login (`pages/auth/Login.js`) → `dispatch(login())` → on success `dispatch(setToken(res.access_token))` (sets token + `isLoggedIn`) and navigate to `/dashboard-main`. The JWT is decoded client-side into `auth.user` (carries `layer` = `TOP` / `RESELLER` / `ACCOUNT`, `role`, `changePassword`). There is **no OAuth/Keycloak runtime flow** despite `keycloak-js` being a dependency — auth is the custom JWT login above.

### Permission-driven navigation
The sidebar is **server-driven**: `fetchMenu()` (`GET /api/v1/menu`) returns `menuData`, and `SideBarOpen.js` recursively maps it (`mnName`, `mnLink`, `children`) into Ant Design menu items; clicking navigates to `mnLink`. Route guarding is coarse (single `EXECUTIVE` role on the route group) — actual feature visibility comes from the menu and from `permissionData` (fetched per role via `fetchPermissions`, used in `UserPermissionModal.jsx`). The three `layer` values drive which dashboard a user lands on (`dashboard-main` / `dashboard-reseller` / `dashboard-account`).

### UI conventions
- **Ant Design (antd v5)** for components (Tables, Modals, notifications) **+ Tailwind** for layout/utility styling — both are used together throughout. Tailwind theme/colors are customized in `tailwind.config.js` (custom tokens like `blu`, `blk3`, `bluePurple`; fonts `dmSans`, `lexendS`). Custom CSS overrides in `src/antd.css`, `src/index.css`, `src/texts.css`, `src/load.css`.
- Toasts: `react-hot-toast` (mounted in `index.js`) with the styled `customToast(...)` helper in `src/utils.js`; antd `notification` via `openNotificationWithIcon(...)`.
- `src/utils.js` is the shared helper grab-bag — currency (`formatMoney`/`cashConverter`, **hardcoded KES**), date formatting/relative-date helpers (`normalizeDateToLocalYear*`, `getDate30DaysAgo`, etc., using `moment` + native `Date`), `formatImgPath` (rewrites `./`-prefixed API paths to `https://sms.smartgate.co.ke`), `getSubdomain` (white-label key), text helpers. Check here before writing new formatting code.
- Components in `src/components/` are shared (modals like `ConfirmationModal`, `DeleteModal`, `LoginModal`, `TableLoading`); page-specific modals live next to their page.

## Conventions & gotchas

- Mixed `.js` / `.jsx` extensions for React components — both are normal; follow the neighboring files in a folder.
- Heavy use of optional chaining (`?.`) on API data because responses are loosely typed and often deeply nested.
- Pagination: list slices store both the array and a `*Count`/`total`; antd `Table` pagination is driven from that count.
- The persisted Redux store means stale shapes survive reloads — when you change a slice's `initialState` in a way that matters, expect users to need a logout/`localStorage.clear()` (logout already clears it).
- `data.js` holds static/mock lookup data used by some pages.
