import { redirect } from '@sveltejs/kit';
import { get } from 'svelte/store';
import { auth } from '$lib/stores/auth';

export function load(): void {
	if (!get(auth).isAuthenticated) {
		throw redirect(302, '/auth/login');
	}
}