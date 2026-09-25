import { redirect } from '@sveltejs/kit';
import { get } from 'svelte/store';
import { auth } from '$lib/stores/auth';
import { canView, canEditRH, canSeeNiveis } from '$lib/utils/permissions';

export const ssr = false;
export const prerender = false;

export function load({ url }: { url: URL }) {
	const user = get(auth).user;

	if (!canView(user, 'rh')) {
		throw redirect(302, `/dashboard?denied=${encodeURIComponent(url.pathname)}`);
	}

	if (url.pathname === '/pessoas/niveis' || url.pathname.startsWith('/pessoas/niveis/')) {
		if (!canSeeNiveis(user)) {
			throw redirect(303, '/pessoas');
		}
	}

	return { canEdit: canEditRH(user) };
}
