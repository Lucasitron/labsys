import type { PageLoad } from './$types';
import { fetchItem } from '$lib/api/stock/items';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ params, fetch }) => {
	try {
		const detail = await fetchItem(fetch, params.id);
		return { detail, error: null as string | null };
	} catch (err) {
		return {
			detail: null,
			error: err instanceof Error ? err.message : 'Não foi possível carregar o item'
		};
	}
};