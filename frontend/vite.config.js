import { defineConfig, transformWithOxc } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'node:url';

// CRA allowed JSX inside .js files; Vite does not by default. Vite 8 swapped
// esbuild for Rolldown/oxc, so we transform JSX in .js files with oxc (the old
// transformWithEsbuild recipe no longer works — esbuild was removed from Vite).
// `enforce: 'pre'` runs this before Rolldown's builtin transform, which would
// otherwise parse these .js files as plain JS and choke on the JSX.
export default defineConfig({
  plugins: [
    {
      name: 'treat-js-files-as-jsx',
      enforce: 'pre',
      async transform(code, id) {
        // Only OUR source .js files carry CRA-style JSX. The previous
        // /src\/.*\.js$/ regex also matched node_modules packages that ship a
        // src/ dir (d3, internmap, …), needlessly oxc-transforming hundreds of
        // plain-JS vendor files on every cold start. Scope to the project src
        // and skip node_modules. (Vite core's oxc can't help here: it infers
        // lang:'js' from the .js extension and disables JSX parsing.)
        const [file] = id.split('?');
        if (
          id.includes('/node_modules/') ||
          !file.includes('/src/') ||
          !file.endsWith('.js')
        ) {
          return null;
        }
        const { code: transformed, map } = await transformWithOxc(code, id, {
          lang: 'jsx',
          jsx: { runtime: 'automatic' },
        });
        return { code: transformed, map };
      },
    },
    react(),
  ],
  resolve: {
    alias: {
      // `material-icons-react` is abandoned and pulled a vulnerable React-15 /
      // fbjs / node-fetch subtree (6 high-severity audit advisories). It is
      // replaced by a local shim that renders the identical <i class="material-icons">.
      // The package has been removed from package.json; this alias keeps the 31
      // existing default imports working unchanged.
      'material-icons-react': fileURLToPath(
        new URL('./src/shims/material-icons-react.jsx', import.meta.url)
      ),
    },
  },
  server: {
    port: 3000,
    open: true,
  },
  // Binary templates imported in code (downloaded by the user). CRA's webpack
  // handled these automatically; Vite needs them declared as static assets.
  assetsInclude: ['**/*.xlsx', '**/*.docx'],
  build: {
    // Match the old CRA output dir so .github/workflows/react.yml (copies frontend/build/*) keeps working.
    outDir: 'build',
  },
  optimizeDeps: {
    // Only scan the real app entry; avoids picking up stray .html files
    // (e.g. src/assets/Fonts/helvetica/example.html) as extra entry points.
    entries: ['index.html'],
    // The dep scanner / pre-bundler is a separate Rolldown pass that does NOT
    // run the `treat-js-files-as-jsx` plugin above, so tell it that .js files
    // may contain JSX (Vite 8 replacement for the old esbuildOptions.loader).
    rolldownOptions: {
      moduleTypes: { '.js': 'jsx' },
    },
  },
});
