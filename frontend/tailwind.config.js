/** @type {import('tailwindcss').Config} */
const defaultTheme = require('tailwindcss/defaultTheme');

module.exports = {
	content: ['./src/**/*.{html,js,jsx}'],
	theme: {
		extend: {
			fontFamily: {
				dmSans: ['DM Sans','sans-serif'],
				lexendS: ['Lexend','sans-serif'], 
				roboto: ['Roboto','sans-serif'],  
				sans: ['Source Sans 3','sans-serif'],  
			},  
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
			darkGreen:"#388E3C",
			syncBtn:"#69472E", 
		},
		screens: {
			xs: '360px',
			...defaultTheme.screens,
		},
	},
	plugins: [],
};
