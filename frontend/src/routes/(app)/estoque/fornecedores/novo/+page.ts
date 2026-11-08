import { redirect } from '@sveltejs/kit';
import { get } from 'svelte/store';
import type { PageLoad } from './$types';
import { auth } from '$lib/stores/auth';
import { canEdit } from '$lib/utils/permissions';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async () => {
	if (!canEdit(get(auth).user, 'estoque')) {
		throw redirect(302, '/estoque/fornecedores');
	}
	return {};
};