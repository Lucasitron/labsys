import { redirect } from '@sveltejs/kit';
import { get } from 'svelte/store';
import { auth, logout, setUser } from '$lib/stores/auth';
import { fetchMe } from '$lib/api/auth';

export const ssr = false;
export const prerender = false;

export async function load({ url, fetch }) {
	const state = get(auth);

	if (!state.isAuthenticated || !state.token) {
		throw redirect(302, `/auth/login?redirect=${encodeURIComponent(url.pathname)}`);
	}

	if (!state.user) {
		try {
			const user = await fetchMe(fetch);
			setUser(user);
		} catch {
			logout();
			throw redirect(302, `/auth/login?redirect=${encodeURIComponent(url.pathname)}`);
		}
	}

	return { user: get(auth).user };
}