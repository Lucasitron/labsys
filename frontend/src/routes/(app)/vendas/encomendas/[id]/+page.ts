import type { PageLoad } from './$types';
import { ApiError } from '$lib/api/client';
import { getEncomenda } from '$lib/api/vendas/encomendas';
import { strParam } from '$lib/utils/stock-url';
import type { Encomenda, HistoricoEncomenda, ItemOrcamento } from '$lib/types/vendas';

export interface EncomendaMarketplaceVinculo {
	plataforma: string;
	codigoExterno: string;
}

// Detalhe servido por GET /api/vendas/encomendas/{id} 🟡.
// Campos além do núcleo de Encomenda são opcionais (nunca calculados no client).
export interface EncomendaDetalhe extends Encomenda {
	itens?: ItemOrcamento[];
	historico?: HistoricoEncomenda[];
	observacoes?: string;
	orcamentoId?: string;
	orcamentoCodigo?: string;
	tempoNaEtapaDias?: number;
	marketplace?: EncomendaMarketplaceVinculo | null;
}

export const ssr = false;
export const prerender = false;

const TABS = ['visao-geral', 'itens', 'historico'];

export const load: PageLoad = async ({ params, url, fetch }) => {
	const tabRaw = strParam(url.searchParams, 'tab');
	const tab = TABS.includes(tabRaw) ? tabRaw : 'visao-geral';

	try {
		const encomenda = (await getEncomenda(params.id, fetch)) as EncomendaDetalhe;
		return { id: params.id, tab, encomenda, notFound: false, error: null as string | null };
	} catch (err) {
		if (err instanceof ApiError && err.status === 404) {
			return { id: params.id, tab, encomenda: null, notFound: true, error: null as string | null };
		}
		return {
			id: params.id,
			tab,
			encomenda: null,
			notFound: false,
			error: 'Não foi possível carregar a encomenda'
		};
	}
};
