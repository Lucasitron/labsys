import { redirect } from '@sveltejs/kit';
import { get } from 'svelte/store';
import type { PageLoad } from './$types';
import type { Fornecedor } from '$lib/types/stock';
import { auth } from '$lib/stores/auth';
import { canEdit } from '$lib/utils/permissions';
import { fetchSupplierOptions } from '$lib/api/stock/suppliers';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ fetch }) => {
	if (!canEdit(get(auth).user, 'estoque')) {
		throw redirect(302, '/estoque/entradas');
	}

	const suppliers = (await fetchSupplierOptions(fetch).catch(() => [] as Fornecedor[])).map((s) => ({
		id: s.id,
		label: s.nome
	}));

	return { suppliers };
};