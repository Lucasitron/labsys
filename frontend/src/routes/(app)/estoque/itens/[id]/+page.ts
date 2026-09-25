import type { PageLoad } from './$types';
import type { ItemDetail } from '$lib/types/stock';
import { buscarItem } from '$lib/api/stock/items';
import { strParam } from '$lib/utils/stock-url';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ params, url }) => {
	const tab = strParam(url.searchParams, 'tab');

	try {
		const detail = (await buscarItem(params.id, fetch)) as ItemDetail;
		return { tab, detail, error: null as string | null };
	} catch (err) {
		return {
			tab,
			detail: null,
			error: err instanceof Error ? err.message : 'Não foi possível carregar o item'
		};
	}
};