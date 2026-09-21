import type { PageLoad } from './$types';
import { listarItens } from '$lib/api/stock/items';
import { localizacaoLabel } from '$lib/utils/stock-status';
import { arrParam, intParam, sanitizeSort, strParam } from '$lib/utils/stock-url';
import type {
	CategoriaEnum,
	FilterOption,
	ItemStatus,
	ItemsResult,
	LocalizacaoResp,
	PageInfo,
	StockItem
} from '$lib/types/stock';

export const ssr = false;
export const prerender = false;

export interface ItensFilterState {
	search: string;
	categories: string[];
	locations: string[];
	statuses: string[];
	page: number;
	pageSize: number;
	sort: string;
}

const SORTS = ['name:asc', 'name:desc', 'qtd:asc', 'qtd:desc'];

const STATUS_OPTIONS: { id: ItemStatus; label: string }[] = [
	{ id: 'available', label: 'Disponível' },
	{ id: 'low', label: 'Baixo' },
	{ id: 'out', label: 'Esgotado' },
	{ id: 'loaned', label: 'Emprestado' },
	{ id: 'maintenance', label: 'Manutenção' }
];

// Categoria é enum do backend (INSUMO|FERRAMENTA|PECA) — dropdown sempre com as 3 opções.
const CATEGORIA_OPTIONS: { id: CategoriaEnum; label: string }[] = [
	{ id: 'INSUMO', label: 'Insumo' },
	{ id: 'FERRAMENTA', label: 'Ferramenta' },
	{ id: 'PECA', label: 'Peça' }
];

function normalize(value: string): string {
	return value.toLowerCase().trim();
}

function applySearch(items: StockItem[], term: string): StockItem[] {
	const t = normalize(term);
	if (!t) return items;
	return items.filter(
		(i) => normalize(i.nome).includes(t) || normalize(i.descricao ?? '').includes(t)
	);
}

function applyFilters(
	items: StockItem[],
	params: ItensFilterState
): { items: StockItem[]; filters: ItemsResult['filters'] } {
	const locIds = [
		...new Set(items.map((i) => i.localizacao?.id).filter((id): id is string => !!id))
	];
	const locs: LocalizacaoResp[] = locIds
		.map((id) => items.map((i) => i.localizacao).find((l) => l?.id === id))
		.filter((l): l is LocalizacaoResp => l !== undefined);
	const statuses: ItemStatus[] = ['available', 'low', 'out', 'loaned', 'maintenance'];

	const categoryOptions: FilterOption[] = CATEGORIA_OPTIONS.map((c) => ({
		id: c.id,
		label: c.label,
		count: items.filter((i) => i.categoria === c.id).length
	}));

	const locationOptions: FilterOption[] = locs.map((l) => ({
		id: l.id,
		label: localizacaoLabel(l),
		count: items.filter((i) => i.localizacao?.id === l.id).length
	}));

	const statusOptions: FilterOption[] = statuses.map((s) => ({
		id: s,
		label: STATUS_OPTIONS.find((o) => o.id === s)?.label ?? s,
		count: items.filter((i) => i.status === s).length
	}));

	const filtered = items.filter((i) => {
		if (params.categories.length > 0 && !params.categories.includes(i.categoria)) return false;
		if (
			params.locations.length > 0 &&
			!(i.localizacao && params.locations.includes(i.localizacao.id))
		)
			return false;
		if (params.statuses.length > 0 && !params.statuses.includes(i.status)) return false;
		return true;
	});

	return {
		items: filtered,
		filters: {
			categories: categoryOptions,
			locations: locationOptions,
			statuses: statusOptions
		}
	};
}

function applySort(items: StockItem[], sort: string): StockItem[] {
	const [key, dir] = sort.split(':');
	const factor = dir === 'desc' ? -1 : 1;
	return [...items].sort((a, b) => {
		if (key === 'qtd') return (a.quantidadeAtual - b.quantidadeAtual) * factor;
		return a.nome.localeCompare(b.nome, 'pt-BR') * factor;
	});
}

function paginate(
	items: StockItem[],
	page: number,
	pageSize: number
): { items: StockItem[]; pagination: PageInfo } {
	const totalItems = items.length;
	const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
	const safePage = Math.min(Math.max(1, page), totalPages);
	const start = (safePage - 1) * pageSize;
	return {
		items: items.slice(start, start + pageSize),
		pagination: { page: safePage, pageSize, totalItems, totalPages }
	};
}

export const load: PageLoad = async ({ url, fetch }) => {
	const searchParams = url.searchParams;
	const params: ItensFilterState = {
		search: strParam(searchParams, 'search'),
		categories: arrParam(searchParams, 'category'),
		locations: arrParam(searchParams, 'location'),
		statuses: arrParam(searchParams, 'status'),
		page: intParam(searchParams, 'page', 1),
		pageSize: intParam(searchParams, 'pageSize', 10, 10, 100),
		sort: sanitizeSort(strParam(searchParams, 'sort'), SORTS)
	};

	try {
		const all = await listarItens({}, fetch);
		const searched = applySearch(all, params.search);
		const { items: filtering, filters } = applyFilters(searched, params);
		const sorted = applySort(filtering, params.sort || 'name:asc');
		const { items, pagination } = paginate(sorted, params.page, params.pageSize);
		const result: ItemsResult = { items, pagination, filters };
		return { params, result, error: null as string | null };
	} catch (err) {
		return { params, result: null, error: err instanceof Error ? err.message : 'Erro' };
	}
};