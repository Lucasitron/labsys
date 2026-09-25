import type { PageLoad } from './$types';
import { listEncomendas } from '$lib/api/vendas/encomendas';
import { strParam } from '$lib/utils/stock-url';
import type {
	EncomendasResult,
	KanbanStatus,
	ListEncomendasParams
} from '$lib/types/vendas';

export interface EncomendasFilterState {
	status_kanban: string;
	search: string;
	clienteId: string;
}

export const ssr = false;
export const prerender = false;

const KANBAN: KanbanStatus[] = ['Fila', 'Produção', 'Acabamento', 'Pronto', 'Entregue'];

export const load: PageLoad = async ({ url, fetch }) => {
	const statusRaw = strParam(url.searchParams, 'status_kanban');
	const status_kanban = (KANBAN as string[]).includes(statusRaw) ? statusRaw : '';

	const params: EncomendasFilterState = {
		status_kanban,
		search: strParam(url.searchParams, 'search'),
		clienteId: strParam(url.searchParams, 'clienteId')
	};

	try {
		const consulta: ListEncomendasParams = {
			...(status_kanban ? { status_kanban: status_kanban as KanbanStatus } : {}),
			...(params.search ? { search: params.search } : {}),
			...(params.clienteId ? { clienteId: params.clienteId } : {})
		};
		const resultado: EncomendasResult = await listEncomendas(consulta, fetch);
		return { params, resultado, error: null as string | null };
	} catch {
		return {
			params,
			resultado: null,
			error: 'Não foi possível carregar as encomendas'
		};
	}
};
