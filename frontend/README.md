# SynqAfrica — Reseller SMS Front-end

Multi-tenant reseller SMS platform UI (React 18 + Vite). White-labeled per reseller
(logos/theme swap by subdomain). The backend is a Java/SMS gateway exposing `/api/v1`
and `/api/v2`; this repo is UI only.

> Migrated from Create React App to **Vite** (build tooling only — app code unchanged).

## Available Scripts

In the project directory, you can run:

### `npm run dev` (or `npm start`)

Runs the app in development mode on [http://localhost:3000](http://localhost:3000).
The page hot-reloads as you edit.

### `npm run build`

Builds the app for production to the `build/` folder (minified, hashed filenames).
This is the directory `.github/workflows/react.yml` copies to the VM on deploy.

### `npm run preview`

Serves the already-built `build/` folder locally to smoke-test a production build
before deploying.

## Environment

`.env` (gitignored) holds Vite env vars — note Vite requires the `VITE_` prefix and
they are read via `import.meta.env`:

- `VITE_API_BASE_URL` — backend base URL, read as `import.meta.env.VITE_API_BASE_URL`.
- `VITE_GOOGLE`

The CI build injects no `.env`, so the deployed bundle ships with these `undefined`
and axios falls back to same-origin requests (nginx on the VM proxies the API).

## Notes on the Vite migration

- JSX is allowed in `.js` files via a small `transformWithEsbuild` plugin in
  `vite.config.js` (CRA permitted this; esbuild does not by default).
- Binary templates imported in code (`.xlsx`, `.docx`) are declared in
  `assetsInclude` in `vite.config.js`.
- Routing uses `createHashRouter` (`/#/...`), so the static nginx deploy needs no
  SPA history-fallback config.
