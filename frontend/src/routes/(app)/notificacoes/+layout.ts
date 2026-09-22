import { redirect } from '@sveltejs/kit';
import { get } from 'svelte/store';
import { auth } from '$lib/stores/auth';
import { isAdmin } from '$lib/utils/permissions';

export const ssr = false;
export const prerender = false;

export function load({ url }: { url: URL }) {
	const user = get(auth).user;
	const path = url.pathname;

	if (
		(path === '/notificacoes/preferencias' || path.startsWith('/notificacoes/preferencias/')) ||
		(path === '/notificacoes/historico' || path.startsWith('/notificacoes/historico/'))
	) {
		if (!isAdmin(user)) {
			throw redirect(303, '/notificacoes');
		}
	}

	return {};
}