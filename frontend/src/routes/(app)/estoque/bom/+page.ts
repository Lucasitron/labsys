import type { PageLoad } from './$types';
import type { BomResponse, StockItem } from '$lib/types/stock';
import { buscarBom } from '$lib/api/stock/bom';
import { listarItens } from '$lib/api/stock/items';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ url, fetch }) => {
	const bomId = (url.searchParams.get('id') ?? '').trim();

	if (!bomId) {
		return { bomId: '', bom: null as BomResponse | null, itens: [] as StockItem[], error: null as string | null };
	}

	// R-8: não há endpoint de listagem de BOM por projeto — o id vem pela URL (?id=…).
	try {
		const [bom, itens] = await Promise.all([buscarBom(bomId, fetch), listarItens({}, fetch)]);
		return { bomId, bom, itens, error: null as string | null };
	} catch (err) {
		return {
			bomId,
			bom: null as BomResponse | null,
			itens: [] as StockItem[],
			error: err instanceof Error ? err.message : 'Não foi possível carregar a lista de materiais'
		};
	}
};