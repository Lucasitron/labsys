import type { PageLoad } from './$types';
import type { ItemDetail } from '$lib/types/stock';
import { buscarItem } from '$lib/api/stock/items';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ params, fetch }) => {
	try {
		const detail = (await buscarItem(params.id, fetch)) as ItemDetail;
		return { detail, error: null as string | null };
	} catch (err) {
		return {
			detail: null,
			error: err instanceof Error ? err.message : 'Não foi possível carregar o item'
		};
	}
};