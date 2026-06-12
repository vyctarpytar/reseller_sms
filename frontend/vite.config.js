import { defineConfig, transformWithEsbuild } from 'vite';
import react from '@vitejs/plugin-react';

// CRA allowed JSX inside .js files; Vite/esbuild does not by default.
// This is the official Vite recipe (https://vite.dev/guide/troubleshooting) to
// transform JSX in .js files for both dev and the Rollup production build.
export default defineConfig({
  plugins: [
    {
      name: 'treat-js-files-as-jsx',
      async transform(code, id) {
        if (!id.match(/src\/.*\.js$/)) return null;
        return transformWithEsbuild(code, id, {
          loader: 'jsx',
          jsx: 'automatic',
        });
      },
    },
    react(),
  ],
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
    esbuildOptions: {
      loader: { '.js': 'jsx' },
    },
  },
});
