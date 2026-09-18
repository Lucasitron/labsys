import type { Config } from 'tailwindcss';

export default {
	content: ['./src/**/*.{html,js,svelte,ts}'],
	theme: {
		extend: {
			colors: {
				base: '#0a0e17',
				surface: '#111725',
				elevated: '#1a2235',
				border: '#252d42',
				ink: '#e6edf3',
				muted: '#8b95a7',
				brand: '#1c80de',
				brandhi: '#3a95e8',
				success: '#0ea641',
				danger: '#f2060a'
			},
			fontFamily: {
				sans: ['Inter', 'system-ui', 'sans-serif']
			}
		}
	},
	plugins: []
} satisfies Config;