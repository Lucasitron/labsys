import type { BomOrderResult, BomReport, ProjectOption } from '$lib/types/stock';
import { buildQuery, stockFetch } from './request';

export async function fetchBom(fetchFn: typeof fetch, projectId: string): Promise<BomReport> {
	const qs = buildQuery({ projectId });
	return stockFetch<BomReport>(`/stock/bom${qs}`, {}, fetchFn);
}

export function generateOrder(bomId: string): Promise<BomOrderResult> {
	return stockFetch<BomOrderResult>(`/stock/bom/${bomId}/pedido`, {
		method: 'POST'
	});
}

export async function fetchProjects(
	fetchFn: typeof fetch,
	search = ''
): Promise<ProjectOption[]> {
	const qs = buildQuery({ search });
	return stockFetch<ProjectOption[]>(`/projects${qs}`, {}, fetchFn);
}