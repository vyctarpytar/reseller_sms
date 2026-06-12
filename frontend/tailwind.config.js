import defaultTheme from 'tailwindcss/defaultTheme';

/** @type {import('tailwindcss').Config} */
export default {
	content: ['./src/**/*.{html,js,jsx}'],
	theme: {
		extend: {
			fontFamily: {
				dmSans: ['DM Sans','sans-serif'],
				lexendS: ['Lexend','sans-serif'],
				roboto: ['Roboto','sans-serif'],
				sans: ['Source Sans 3','sans-serif'],
			},
			boxShadow: {
				card: '0 4px 6px -1px rgba(0,0,0,.1), 0 2px 4px -1px rgba(0,0,0,.06)',
				lift: '0 20px 25px -5px rgba(0,0,0,.1), 0 10px 10px -5px rgba(0,0,0,.04)',
			},
			borderRadius: { card: '12px' },
		},
		colors: { 
			blk14: '#141414',
			blk: '#000',
			blk3: '#333',
			blk2: '#222',
			blk2B:'#2B2B2B',
			blk21:'#212121',
			gray: '#d8cfcf',
			ltPpl: '#efeef6',
			white: "#fff",
			blu: "#285ff6",
			wyt: "#fefefa",
			green:'#0b795d',
			red:'#e5181c',
			bluePurple: '#7114EF',
			darkBlue: "#147CBC",
			blue: "#0873B9",
			lightBlue: "#EDF8FF",
			// Synq Africa brand palette (synqafrica.co.ke)
			saOrange: "#D96C3B", // primary accent (terracotta)
			saBrown: "#69472E",  // secondary ("Synq" wordmark brown)
			saDark: "#13161D",   // dark surfaces
			darkGreen:"#69472E", // legacy alias → retargeted to SA brand brown (app-wide accent)
			syncBtn:"#69472E",
			// Design-system roles (frontend-design skill)
			primary: "#69472E",
			accent: { DEFAULT: "#D96C3B", hover: "#b8562a" },
			surface: "#fafaf9",
			muted: "#64748b",
			ink: "#334155",
			border: "rgba(0,0,0,0.06)",
		},
		screens: {
			xs: '360px',
			...defaultTheme.screens,
		},
	},
	plugins: [],
};
