import { redirect } from '@sveltejs/kit';
import { get } from 'svelte/store';
import type { PageLoad } from './$types';
import type { ProjectOption } from '$lib/types/stock';
import { auth } from '$lib/stores/auth';
import { canEdit } from '$lib/utils/permissions';
import { fetchProjects } from '$lib/api/stock/bom';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ fetch }) => {
	if (!canEdit(get(auth).user, 'estoque')) {
		throw redirect(302, '/estoque/saidas');
	}

	const projects = (await fetchProjects(fetch).catch(() => [] as ProjectOption[])).map((p) => ({
		id: p.id,
		label: p.name
	}));

	return { projects };
};