import type { PageLoad } from './$types';
import { fetchItems } from '$lib/api/stock/items';
import { arrParam, intParam, sanitizeSort, strParam } from '$lib/utils/stock-url';

export const ssr = false;
export const prerender = false;

export interface ItensFilterState {
	search: string;
	categories: string[];
	locations: string[];
	statuses: string[];
	page: number;
	pageSize: number;
	sort: string;
}

const SORTS = [
	'name:asc',
	'name:desc',
	'code:asc',
	'code:desc',
	'qtd:asc',
	'qtd:desc',
	'updated:asc',
	'updated:desc'
];

export const load: PageLoad = async ({ url, fetch }) => {
	const searchParams = url.searchParams;
	const params: ItensFilterState = {
		search: strParam(searchParams, 'search'),
		categories: arrParam(searchParams, 'category'),
		locations: arrParam(searchParams, 'location'),
		statuses: arrParam(searchParams, 'status'),
		page: intParam(searchParams, 'page', 1),
		pageSize: intParam(searchParams, 'pageSize', 10, 10, 100),
		sort: sanitizeSort(strParam(searchParams, 'sort'), SORTS)
	};

	try {
		const result = await fetchItems(fetch, {
			...params,
			sort: params.sort || undefined
		});
		return { params, result, error: null as string | null };
	} catch (err) {
		return { params, result: null, error: err instanceof Error ? err.message : 'Erro' };
	}
};