import type { PageLoad } from './$types';
import { listOrcamentos } from '$lib/api/vendas/orcamentos';
import { intParam, strParam } from '$lib/utils/stock-url';
import type {
	ListOrcamentosParams,
	OrcamentoStatus,
	OrcamentosResult
} from '$lib/types/vendas';

export interface OrcamentosFilterState {
	tab: string;
	search: string;
	page: number;
	pageSize: number;
}

export const ssr = false;
export const prerender = false;

const TABS = ['todos', 'pendentes', 'aprovados', 'ajuste', 'recusados'];

const STATUS_POR_TAB: Record<string, OrcamentoStatus | undefined> = {
	todos: undefined,
	pendentes: 'Pendente',
	aprovados: 'Aprovado',
	ajuste: 'Ajuste',
	recusados: 'Recusado'
};

export const load: PageLoad = async ({ url, fetch }) => {
	const tabRaw = strParam(url.searchParams, 'tab');
	const tab = TABS.includes(tabRaw) ? tabRaw : 'todos';

	const params: OrcamentosFilterState = {
		tab,
		search: strParam(url.searchParams, 'search'),
		page: intParam(url.searchParams, 'page', 1),
		pageSize: intParam(url.searchParams, 'pageSize', 10, 10, 100)
	};

	try {
		const consulta: ListOrcamentosParams = {
			search: params.search || undefined,
			page: params.page,
			pageSize: params.pageSize
		};
		const status = STATUS_POR_TAB[tab];
		if (status) consulta.status = status;
		const resultado: OrcamentosResult = await listOrcamentos(consulta, fetch);
		return { params, resultado, error: null as string | null };
	} catch {
		return {
			params,
			resultado: null,
			error: 'Não foi possível carregar os orçamentos'
		};
	}
};
