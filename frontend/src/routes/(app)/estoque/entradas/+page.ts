import type { PageLoad } from './$types';
import { arrParam, intParam, strParam } from '$lib/utils/stock-url';
import type { MovementListState } from '$lib/components/estoque/MovementList.svelte';
import type { MovementsResult } from '$lib/types/stock';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ url }) => {
	const searchParams = url.searchParams;
	const params: MovementListState = {
		search: strParam(searchParams, 'search'),
		keys: arrParam(searchParams, 'kind'),
		period: strParam(searchParams, 'period'),
		page: intParam(searchParams, 'page', 1),
		pageSize: intParam(searchParams, 'pageSize', 10, 10, 100)
	};

	// R-9: backend não expõe listagem global de movimentações (só por item). Bloco 2
	// implementa o histórico agregado; enquanto isso a tela mostra o estado vazio.
	const result: MovementsResult = {
		movements: [],
		pagination: { page: 1, pageSize: params.pageSize, totalItems: 0, totalPages: 1 },
		filters: { kinds: [], reasons: [], periods: [] }
	};

	return { params, result, error: null as string | null };
};