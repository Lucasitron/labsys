import type { PageLoad } from './$types';
import { listMarketplace } from '$lib/api/vendas/marketplace';
import { intParam, strParam } from '$lib/utils/stock-url';
import type {
	ListMarketplaceParams,
	MarketplaceResult,
	Plataforma
} from '$lib/types/vendas';

export interface MarketplaceFilterState {
	tab: string;
	page: number;
	pageSize: number;
}

export const ssr = false;
export const prerender = false;

const TABS = ['todos', 'ml', 'shopee', 'elo7'];

const PLATAFORMA_POR_TAB: Record<string, Plataforma | undefined> = {
	todos: undefined,
	ml: 'Mercado Livre',
	shopee: 'Shopee',
	elo7: 'Elo7'
};

export const load: PageLoad = async ({ url, fetch }) => {
	const tabRaw = strParam(url.searchParams, 'tab');
	const tab = TABS.includes(tabRaw) ? tabRaw : 'todos';

	const params: MarketplaceFilterState = {
		tab,
		page: intParam(url.searchParams, 'page', 1),
		pageSize: intParam(url.searchParams, 'pageSize', 10, 10, 100)
	};

	try {
		const consulta: ListMarketplaceParams = {
			page: params.page,
			pageSize: params.pageSize
		};
		const plataforma = PLATAFORMA_POR_TAB[tab];
		if (plataforma) consulta.plataforma = plataforma;
		const resultado: MarketplaceResult = await listMarketplace(consulta, fetch);
		return { params, resultado, error: null as string | null };
	} catch {
		return {
			params,
			resultado: null,
			error: 'Não foi possível carregar as vendas de marketplace'
		};
	}
};
