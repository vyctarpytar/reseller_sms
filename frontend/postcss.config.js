import tailwindcss from 'tailwindcss';
import autoprefixer from 'autoprefixer';

// CRA auto-enabled Tailwind from tailwind.config.js; Vite needs this explicit
// PostCSS config to run Tailwind + Autoprefixer over the @tailwind directives.
export default {
  plugins: [tailwindcss, autoprefixer],
};
