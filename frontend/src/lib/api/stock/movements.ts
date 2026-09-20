import type { CreateMovementPayload, MovementParams, MovementsResult } from '$lib/types/stock';
import { buildQuery, stockFetch } from './request';

const BASE = '/stock/movements';

export async function fetchMovements(
	fetchFn: typeof fetch,
	params: MovementParams
): Promise<MovementsResult> {
	const qs = buildQuery({
		type: params.type,
		page: params.page,
		pageSize: params.pageSize,
		search: params.search,
		kind: params.kinds,
		reason: params.reasons,
		period: params.period,
		itemId: params.itemId
	});
	return stockFetch<MovementsResult>(`${BASE}${qs}`, {}, fetchFn);
}

export function createMovement(payload: CreateMovementPayload): Promise<unknown> {
	return stockFetch<unknown>(BASE, {
		method: 'POST',
		body: JSON.stringify(payload)
	});
}