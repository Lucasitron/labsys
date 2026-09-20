import type {
	Category,
	CreateItemPayload,
	HistoryEntry,
	ItemDetail,
	ItemMovementsResult,
	ItemStatus,
	ItemsResult,
	Loan,
	StockItem,
	StockParams,
	Supplier
} from '$lib/types/stock';
import { buildQuery, stockFetch, stockFetchBlob } from './request';

const BASE = '/stock/items';

export async function fetchItems(
	fetchFn: typeof fetch,
	params: StockParams
): Promise<ItemsResult> {
	const qs = buildQuery({
		page: params.page,
		pageSize: params.pageSize,
		search: params.search,
		category: params.categories,
		location: params.locations,
		status: params.statuses,
		sort: params.sort
	});
	return stockFetch<ItemsResult>(`${BASE}${qs}`, {}, fetchFn);
}

export async function fetchItem(fetchFn: typeof fetch, id: string): Promise<ItemDetail> {
	return stockFetch<ItemDetail>(`${BASE}/${id}`, {}, fetchFn);
}

export async function fetchItemMovements(
	fetchFn: typeof fetch,
	id: string,
	page = 1
): Promise<ItemMovementsResult> {
	return stockFetch<ItemMovementsResult>(
		`/stock/movements?itemId=${id}&page=${page}&pageSize=8`,
		{},
		fetchFn
	);
}

export async function fetchItemLoans(fetchFn: typeof fetch, id: string): Promise<Loan[]> {
	return stockFetch<Loan[]>(`/stock/loans?itemId=${id}`, {}, fetchFn);
}

export async function fetchItemSuppliers(fetchFn: typeof fetch, id: string): Promise<Supplier[]> {
	return stockFetch<Supplier[]>(`${BASE}/${id}/suppliers`, {}, fetchFn);
}

export async function fetchItemHistory(
	fetchFn: typeof fetch,
	id: string
): Promise<HistoryEntry[]> {
	const result = await stockFetch<{ history: HistoryEntry[] }>(
		`${BASE}/${id}/history`,
		{},
		fetchFn
	);
	return result.history;
}

export async function fetchCategories(fetchFn: typeof fetch): Promise<Category[]> {
	return stockFetch<Category[]>('/stock/categories', {}, fetchFn);
}

export function fetchStatuses(): { id: ItemStatus; label: string }[] {
	return [
		{ id: 'available', label: 'Disponível' },
		{ id: 'low', label: 'Baixo' },
		{ id: 'out', label: 'Esgotado' },
		{ id: 'loaned', label: 'Emprestado' },
		{ id: 'maintenance', label: 'Manutenção' }
	];
}

export function createItem(payload: CreateItemPayload): Promise<StockItem> {
	return stockFetch<StockItem>(BASE, {
		method: 'POST',
		body: JSON.stringify(payload)
	});
}

export function updateItem(id: string, payload: Partial<CreateItemPayload>): Promise<StockItem> {
	return stockFetch<StockItem>(`${BASE}/${id}`, {
		method: 'PUT',
		body: JSON.stringify(payload)
	});
}

export function deleteItem(id: string): Promise<void> {
	return stockFetch<void>(`${BASE}/${id}`, { method: 'DELETE' });
}

export interface BulkResult {
	updated: number;
	failed: string[];
}

export function bulkAction(
	ids: string[],
	action: 'updateCategory' | 'delete',
	value?: string
): Promise<BulkResult> {
	return stockFetch<BulkResult>('/stock/items/bulk', {
		method: 'PATCH',
		body: JSON.stringify({ ids, action, value })
	});
}

export async function exportItemsCsv(params: StockParams, fetchFn: typeof fetch = fetch): Promise<void> {
	const qs = buildQuery({
		search: params.search,
		category: params.categories,
		location: params.locations,
		status: params.statuses,
		sort: params.sort
	});
	const blob = await stockFetchBlob(`${BASE}/export${qs}`, {}, fetchFn);
	const url = URL.createObjectURL(blob);
	const link = document.createElement('a');
	link.href = url;
	link.download = 'itens-do-estoque.csv';
	link.click();
	URL.revokeObjectURL(url);
}