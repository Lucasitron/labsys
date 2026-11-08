import type { PageLoad } from './$types';
import { listTarefas } from '$lib/api/vendas/marketing';
import { intParam, strParam } from '$lib/utils/stock-url';
import type {
	ListTarefasParams,
	Prioridade,
	TarefaStatus,
	TarefasResult
} from '$lib/types/vendas';

export interface MarketingFilterState {
	search: string;
	status: string;
	prioridade: string;
	page: number;
	pageSize: number;
}

export const ssr = false;
export const prerender = false;

const STATUS_VALIDOS: TarefaStatus[] = ['Pendente', 'Em Andamento', 'Concluída'];
const PRIORIDADES_VALIDAS: Prioridade[] = ['Baixa', 'Média', 'Alta'];

export const load: PageLoad = async ({ url, fetch }) => {
	const statusRaw = strParam(url.searchParams, 'status');
	const prioridadeRaw = strParam(url.searchParams, 'prioridade');

	const params: MarketingFilterState = {
		search: strParam(url.searchParams, 'search'),
		status: (STATUS_VALIDOS as string[]).includes(statusRaw) ? statusRaw : '',
		prioridade: (PRIORIDADES_VALIDAS as string[]).includes(prioridadeRaw) ? prioridadeRaw : '',
		page: intParam(url.searchParams, 'page', 1),
		pageSize: intParam(url.searchParams, 'pageSize', 10, 10, 100)
	};

	try {
		const consulta: ListTarefasParams = {
			page: params.page,
			pageSize: params.pageSize
		};
		if (params.search) consulta.search = params.search;
		if (params.status) consulta.status = params.status as TarefaStatus;
		if (params.prioridade) consulta.prioridade = params.prioridade as Prioridade;
		const resultado: TarefasResult = await listTarefas(consulta, fetch);
		return { params, resultado, error: null as string | null };
	} catch {
		return {
			params,
			resultado: null,
			error: 'Não foi possível carregar as tarefas de marketing'
		};
	}
};
