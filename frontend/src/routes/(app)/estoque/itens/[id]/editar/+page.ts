import { redirect } from '@sveltejs/kit';
import { get } from 'svelte/store';
import type { PageLoad } from './$types';
import type { IdLabel, StockItem } from '$lib/types/stock';
import { ApiError } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import { canEdit } from '$lib/utils/permissions';
import { buscarItem } from '$lib/api/stock/items';
import { listarLocalizacoes } from '$lib/api/stock/locations';
import { localizacaoLabel } from '$lib/utils/stock-status';

export const ssr = false;
export const prerender = false;

const CATEGORIAS: IdLabel[] = [
	{ id: 'INSUMO', label: 'Insumo' },
	{ id: 'FERRAMENTA', label: 'Ferramenta' },
	{ id: 'PECA', label: 'Peça' }
];

export const load: PageLoad = async ({ params, fetch }) => {
	if (!canEdit(get(auth).user, 'estoque')) {
		throw redirect(302, `/estoque/itens/${params.id}`);
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

	try {
		const item = await buscarItem(params.id, fetch);
		return {
			item,
			categories: CATEGORIAS,
			locations,
			error: null as string | null,
			notFound: false
		};
	} catch (err) {
		const notFound = err instanceof ApiError && err.status === 404;
		return {
			item: null as StockItem | null,
			categories: CATEGORIAS,
			locations,
			error: err instanceof Error ? err.message : 'Não foi possível carregar o item',
			notFound
		};
	}
};