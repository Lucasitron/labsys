import { redirect } from '@sveltejs/kit';
import { get } from 'svelte/store';
import { auth } from '$lib/stores/auth';
import { isAdmin } from '$lib/utils/permissions';

export const ssr = false;
export const prerender = false;

export function load({ url }): { isAdmin: boolean } {
	const user = get(auth).user;

	if (url.pathname !== '/configuracoes/perfil' && !isAdmin(user)) {
		throw redirect(303, '/configuracoes/perfil');
	}

	return { isAdmin: isAdmin(user) };
}