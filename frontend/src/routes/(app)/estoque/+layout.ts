import { redirect } from '@sveltejs/kit';
import { get } from 'svelte/store';
import { auth } from '$lib/stores/auth';
import { canView, canEdit } from '$lib/utils/permissions';

export const ssr = false;
export const prerender = false;

export function load({ url }) {
	const user = get(auth).user;

	if (!canView(user, 'estoque')) {
		throw redirect(302, `/dashboard?denied=${encodeURIComponent(url.pathname)}`);
	}

	return { canEdit: canEdit(user, 'estoque') };
}