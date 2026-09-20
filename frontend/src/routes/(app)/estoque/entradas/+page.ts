import type { PageLoad } from './$types';
import { fetchMovements } from '$lib/api/stock/movements';
import { arrParam, intParam, strParam } from '$lib/utils/stock-url';
import type { MovementListState } from '$lib/components/estoque/MovementList.svelte';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ url, fetch }) => {
	const searchParams = url.searchParams;
	const params: MovementListState = {
		search: strParam(searchParams, 'search'),
		keys: arrParam(searchParams, 'kind'),
		period: strParam(searchParams, 'period'),
		page: intParam(searchParams, 'page', 1),
		pageSize: intParam(searchParams, 'pageSize', 10, 10, 100)
	};

	try {
		const result = await fetchMovements(fetch, {
			type: 'in',
			page: params.page,
			pageSize: params.pageSize,
			search: params.search,
			kinds: params.keys,
			reasons: [],
			period: params.period
		});
		return { params, result, error: null as string | null };
	} catch (err) {
		return {
			params,
			result: null,
			error: err instanceof Error ? err.message : 'Erro ao carregar entradas'
		};
	}
};