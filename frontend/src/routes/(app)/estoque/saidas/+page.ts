import type { PageLoad } from './$types';
import { arrParam, intParam, strParam } from '$lib/utils/stock-url';
import type { MovementListState } from '$lib/components/estoque/MovementList.svelte';
import type { MovementsResult, Movement, SaidaResponse, StockItem } from '$lib/types/stock';
import { buscarItem } from '$lib/api/stock/items';
import { listarSaidasPorItem } from '$lib/api/stock/movements';

export const ssr = false;
export const prerender = false;

interface ExitLine extends Movement {
	id: string;
}

function toM(e: SaidaResponse, unit: string): ExitLine {
	return {
		id: e.id,
		type: 'out',
		reason: e.tipoSaida,
		item: { id: e.idItem, name: e.nomeItem ?? e.idItem, unit },
		quantity: e.quantidade,
		date: e.dataSaida,
		reference: e.idReferencia ?? undefined,
		observation: e.observacao ?? undefined
	};
}

function normalize(value: string): string {
	return value
		.normalize('NFD')
		.replace(/[\u0300-\u036f]/g, '')
		.toLowerCase();
}

function matches(e: ExitLine, term: string): boolean {
	const t = normalize(term);
	if (!t) return true;
	return (
		normalize(e.item.name).includes(t) ||
		normalize(e.destination ?? '').includes(t) ||
		normalize(e.reference ?? '').includes(t)
	);
}

function inPeriod(iso: string, period: string): boolean {
	if (!period) return true;
	const date = new Date(iso);
	const now = new Date();
	const start = new Date(now.getFullYear(), now.getMonth(), now.getDate());
	if (period === 'today') return date >= start;
	if (period === '7d') return date >= new Date(start.getTime() - 6 * 86_400_000);
	if (period === '30d') return date >= new Date(start.getTime() - 29 * 86_400_000);
	if (period === 'year') return date >= new Date(now.getFullYear(), 0, 1);
	return true;
}

function emptyResult(pageSize: number): MovementsResult {
	return {
		movements: [],
		pagination: { page: 1, pageSize, totalItems: 0, totalPages: 1 },
		filters: { kinds: [], reasons: [], periods: [] }
	};
}

export const load: PageLoad = async ({ url, fetch }) => {
	const searchParams = url.searchParams;
	const idItem = strParam(searchParams, 'idItem');
	const params: MovementListState = {
		search: strParam(searchParams, 'search'),
		keys: arrParam(searchParams, 'reason'),
		period: strParam(searchParams, 'period'),
		page: intParam(searchParams, 'page', 1),
		pageSize: intParam(searchParams, 'pageSize', 10, 10, 100)
	};

	if (!idItem) {
		return { idItem: '', item: null as StockItem | null, params, result: emptyResult(params.pageSize), error: null as string | null };
	}

	try {
		const [item, saidas] = await Promise.all([
			buscarItem(idItem, fetch),
			listarSaidasPorItem(idItem, fetch)
		]);

		let lines = saidas.map((e) => toM(e, item.unidadeMedida));
		if (params.search) lines = lines.filter((l) => matches(l, params.search));
		if (params.period) lines = lines.filter((l) => inPeriod(l.date, params.period));
		lines.sort((a, b) => +new Date(b.date) - +new Date(a.date));

		const totalItems = lines.length;
		const totalPages = Math.max(1, Math.ceil(totalItems / params.pageSize));
		const page = Math.min(params.page, totalPages);
		const rows = lines.slice((page - 1) * params.pageSize, page * params.pageSize);

		const result: MovementsResult = {
			movements: rows,
			pagination: { page, pageSize: params.pageSize, totalItems, totalPages },
			filters: { kinds: [], reasons: [], periods: [] }
		};
		return { idItem, item, params, result, error: null as string | null };
	} catch (err) {
		return {
			idItem,
			item: null as StockItem | null,
			params,
			result: emptyResult(params.pageSize),
			error: err instanceof Error ? err.message : 'Não foi possível carregar as saídas'
		};
	}
};