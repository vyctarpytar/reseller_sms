# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Multi-tenant **reseller SMS platform** front-end (React 18 + Vite, migrated from Create React App). The `package.json` name is `synqafrica`; the product is white-labeled per reseller (logos, side images, and theming swap based on the subdomain — see `getSubdomain()` and the `synctel`/`futuresoft`/`sync` asset sets in `src/assets/img/`). The backend is a Java/SMS gateway exposing `/api/v1` and `/api/v2` endpoints; this repo is UI only.

## Commands

```bash
npm run dev        # dev server on http://localhost:3000 (Vite); npm start is an alias
npm run build      # production build to /build (Vite, outDir overridden to build)
npm run preview    # serve the built /build folder to smoke-test before deploy
```

There is no lint or test script (the CRA `react-scripts test`/Jest setup was dropped in the Vite migration; CI only builds). `vite.config.js` carries the migration-specific config: a `treat-js-files-as-jsx` plugin (`enforce: 'pre'`, using `transformWithOxc`) so JSX is allowed in `.js` files, `optimizeDeps.rolldownOptions.moduleTypes` so the dev dep-scanner also accepts JSX-in-`.js`, and `assetsInclude` for `.xlsx`/`.docx` imports. Vite 8 replaced esbuild with Rolldown/oxc, so neither `transformWithEsbuild` nor `optimizeDeps.esbuildOptions` work anymore.

### Environment

`.env` (gitignored, already present locally) holds Vite env vars (must use the `VITE_` prefix, read via `import.meta.env`):
- `VITE_API_BASE_URL` — backend base URL; read as `import.meta.env.VITE_API_BASE_URL` in every slice. **This is the source of truth for the API host**, not `src/config/constant.js` (whose `BASE_URL` constant is stale/unused by the axios layer). Note: CI injects no `.env`, so the deployed bundle ships with this `undefined` and axios falls back to same-origin (nginx proxies the API).
- `VITE_GOOGLE`.

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
- **Request interceptor injects multi-tenancy params on every call**: `reseller_id` / `account_id` from the tenant scope (see below). A call can opt out with `{ skipTenantScope: true }` in its axios config — the reseller switcher does, because `GET api/v2/res` narrows to `reseller_id` when it is sent.
- **Response interceptor never tears anything down on its own.** The backend answers 403 both for "you may not do this" (its JSON envelope, `success:false`) and for a dead token (Spring Security: bare 403 — revoked by a newer login of the same user, expired, missing); an error it fails to handle also surfaces as a bare 403. So a bare 403 / 401 is first checked against `GET api/v1/menu` with the same token: only if that is refused too does it call the handler `Root.js` registers (`setSessionEndedHandler`), which dispatches `logout` and routes to `/login`. Do not put `localStorage.clear()` back here — it used to wipe the tenant scope on any refused request.
- The bearer token is set separately in `ProtectedRoute.js` via `axiosInstance.defaults.headers.common["Authorization"]`.
- Some thunks (login, password reset, `save` in `authSlice`) call raw `axios` instead of `axiosInstance` — intentional for unauthenticated/pre-token calls.

### Tenant scope — `src/custom_hooks/useTenantScope.js`
Which reseller / account a TOP or RESELLER user is acting as. **Per tab, in `sessionStorage`** (`selectedOrg`, `selectedOrgName`, `selectedAccount`, `selectedAccountName`), behind one in-memory snapshot read with `useSyncExternalStore`. That module is the only reader and writer: use `useTenantScope()` in components, `getTenantScope()` outside React, `setTenantScope()` / `clearTenantScope()` to change it — never touch the keys directly, and never keep a copy in `useState`. A store middleware (`tenantScopeGuard` in `app/store.js`) clears it on `auth/logout` and `auth/clearAuthObj`, so every logout path and the login page start clean. It is deliberately not in `localStorage`: that is shared by every tab and wiped wholesale by the login page and by logout, which reset the reseller another tab was working in. Screens branch on `useActingLayer(user.layer)` (TOP inside a reseller acts as RESELLER), not on `user.layer`.

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
