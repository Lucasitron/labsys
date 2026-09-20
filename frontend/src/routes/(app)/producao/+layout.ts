import { redirect } from '@sveltejs/kit';
import { get } from 'svelte/store';
import { auth } from '$lib/stores/auth';
import {
	canAuditar5S,
	canEditProducao,
	canSee5S,
	canSeeAdvertencias,
	canView
} from '$lib/utils/permissions';

export const ssr = false;
export const prerender = false;

export function load({ url }) {
	const user = get(auth).user;

	if (!canView(user, 'producao')) {
		throw redirect(303, `/dashboard?denied=${encodeURIComponent(url.pathname)}`);
	}

	if (url.pathname.startsWith('/producao/5s/')) {
		if (!canSee5S(user)) {
			throw redirect(303, '/producao');
		}

		if (url.pathname.startsWith('/producao/5s/advertencias') && !canSeeAdvertencias(user)) {
			throw redirect(303, '/producao/5s/ranking');
		}
	}

	return {
		user,
		canSee5S: canSee5S(user),
		canAuditar5S: canAuditar5S(user),
		canSeeAdvertencias: canSeeAdvertencias(user),
		canEditProducao: canEditProducao(user)
	};
}