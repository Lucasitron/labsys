import { redirect } from '@sveltejs/kit';
import { get } from 'svelte/store';
import { auth } from '$lib/stores/auth';
import { canDecideVendas, canView, isAdmin } from '$lib/utils/permissions';

export const ssr = false;
export const prerender = false;

export function load({ url }: { url: URL }) {
	const user = get(auth).user;

	if (!canView(user, 'vendas')) {
		throw redirect(302, `/dashboard?denied=${encodeURIComponent(url.pathname)}`);
	}

	const path = url.pathname;
	if (
		(path === '/vendas/solicitacoes' || path.startsWith('/vendas/solicitacoes/')) &&
		!canDecideVendas(user)
	) {
		throw redirect(303, '/vendas/clientes');
	}

	return { canEdit: isAdmin(user) };
}
