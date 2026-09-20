import type { SuppliersResult } from '$lib/types/stock';
import { buildQuery, stockFetch } from './request';

export async function fetchSuppliers(
	fetchFn: typeof fetch,
	params: { page: number; pageSize: number; search: string }
): Promise<SuppliersResult> {
	const qs = buildQuery({
		page: params.page,
		pageSize: params.pageSize,
		search: params.search
	});
	return stockFetch<SuppliersResult>(`/stock/suppliers${qs}`, {}, fetchFn);
}

export async function fetchSupplierOptions(fetchFn: typeof fetch): Promise<
	{ id: string; name: string }[]
> {
	const result = await stockFetch<SuppliersResult>(
		'/stock/suppliers?page=1&pageSize=500',
		{},
		fetchFn
	);
	return (result.suppliers ?? []).map((s) => ({ id: s.id, name: s.name }));
}