import type { PageLoad } from './$types';
import { ApiError } from '$lib/api/client';
import { getCliente } from '$lib/api/vendas/clientes';
import { strParam } from '$lib/utils/stock-url';
import type { Cliente } from '$lib/types/vendas';

export interface ClienteKpisServidos {
	totalEncomendado?: number | null;
	orcamentosAbertos?: number | null;
	encomendasProducao?: number | null;
	ultimaInteracao?: string | null;
}

export type ClienteComKpis = Cliente & { kpis?: ClienteKpisServidos };

export const ssr = false;
export const prerender = false;

const TABS = ['visao-geral', 'interacoes', 'orcamentos', 'encomendas'];

export const load: PageLoad = async ({ params, url, fetch }) => {
	const tabRaw = strParam(url.searchParams, 'tab');
	const tab = TABS.includes(tabRaw) ? tabRaw : 'visao-geral';

	try {
		const cliente = (await getCliente(params.id, fetch)) as ClienteComKpis;
		return { id: params.id, tab, cliente, notFound: false, error: null as string | null };
	} catch (err) {
		if (err instanceof ApiError && err.status === 404) {
			return { id: params.id, tab, cliente: null, notFound: true, error: null as string | null };
		}
		return {
			id: params.id,
			tab,
			cliente: null,
			notFound: false,
			error: 'Não foi possível carregar o cliente'
		};
	}
};
