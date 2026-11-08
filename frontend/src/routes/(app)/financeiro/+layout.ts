import { redirect } from '@sveltejs/kit';
import { get } from 'svelte/store';
import { auth } from '$lib/stores/auth';
import { isAdmin } from '$lib/utils/permissions';

export const ssr = false;
export const prerender = false;

export function load(): void {
	const user = get(auth).user;

	if (!isAdmin(user)) {
		throw redirect(303, '/dashboard');
	}
}
