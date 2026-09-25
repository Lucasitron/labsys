/// <reference types="vitest/config" />
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vitest/config';

export default defineConfig({
	plugins: [sveltekit()],

	server: {
		port: 5173,
		proxy: {
			'/api': {
				target: 'http://localhost:8080',
				changeOrigin: true
			}
		}
	},

	test: {
		include: ['tests/unit/**/*.{test,spec}.{js,ts}'],
		environment: 'jsdom',
		alias: [
			{
				find: /^svelte$/,
				replacement: decodeURIComponent(
					new URL('./node_modules/svelte/src/index-client.js', import.meta.url).pathname
				)
			}
		],
		server: {
			deps: {
				inline: true
			}
		}
	}
});