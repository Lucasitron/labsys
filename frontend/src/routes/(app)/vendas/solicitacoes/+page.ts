import type { PageLoad } from './$types';
import { listSolicitacoes } from '$lib/api/vendas/solicitacoes';
import { strParam } from '$lib/utils/stock-url';
import type { SolicitacaoStatus, SolicitacoesResult } from '$lib/types/vendas';

export interface SolicitacoesFilterState {
	tab: string;
}

export const ssr = false;
export const prerender = false;

const TABS = ['pendentes', 'aprovadas', 'rejeitadas'];

const STATUS_POR_TAB: Record<string, SolicitacaoStatus> = {
	pendentes: 'Pendente',
	aprovadas: 'Aprovada',
	rejeitadas: 'Rejeitada'
};

export const load: PageLoad = async ({ url, fetch }) => {
	const tabRaw = strParam(url.searchParams, 'tab');
	const tab = TABS.includes(tabRaw) ? tabRaw : 'pendentes';

	const params: SolicitacoesFilterState = { tab };

	try {
		const status = STATUS_POR_TAB[tab];
		const resultado: SolicitacoesResult = await listSolicitacoes({ status }, fetch);
		return { params, resultado, error: null as string | null };
	} catch {
		return {
			params,
			resultado: null,
			error: 'Não foi possível carregar as solicitações'
		};
	}
};
