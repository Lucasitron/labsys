import { redirect } from '@sveltejs/kit';
import { get } from 'svelte/store';
import type { PageLoad } from './$types';
import { auth } from '$lib/stores/auth';
import { canEdit } from '$lib/utils/permissions';
import { fetchCategories } from '$lib/api/stock/items';
import { fetchLocationOptions } from '$lib/api/stock/locations';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ fetch }) => {
	if (!canEdit(get(auth).user, 'estoque')) {
		throw redirect(302, '/estoque/itens');
	}

	let categories;
	let locations;
	try {
		[categories, locations] = await Promise.all([
			fetchCategories(fetch).catch(() => [] as never[]),
			fetchLocationOptions(fetch).catch(() => [] as never[])
		]);
	} catch {
		categories = [];
		locations = [];
	}

	return { categories, locations };
};