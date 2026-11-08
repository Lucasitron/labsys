import { redirect } from '@sveltejs/kit';
import { get } from 'svelte/store';
import type { PageLoad } from './$types';
import type { IdLabel } from '$lib/types/stock';
import { auth } from '$lib/stores/auth';
import { canEdit } from '$lib/utils/permissions';
import { listarLocalizacoes } from '$lib/api/stock/locations';
import { localizacaoLabel } from '$lib/utils/stock-status';

export const ssr = false;
export const prerender = false;

const CATEGORIAS: IdLabel[] = [
	{ id: 'INSUMO', label: 'Insumo' },
	{ id: 'FERRAMENTA', label: 'Ferramenta' },
	{ id: 'PECA', label: 'Peça' }
];

export const load: PageLoad = async ({ fetch }) => {
	if (!canEdit(get(auth).user, 'estoque')) {
		throw redirect(302, '/estoque/itens');
	}

	let locations: IdLabel[] = [];
	try {
		locations = (await listarLocalizacoes(fetch)).map((l) => ({
			id: l.id,
			label: localizacaoLabel(l)
		}));
	} catch {
		locations = [];
	}

	return { categories: CATEGORIAS, locations };
};