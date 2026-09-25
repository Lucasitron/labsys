import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	ListMarketplaceParams,
	MarketplaceResult,
	RegistrarVendaPayload,
	RegistroMarketplace
} from '$lib/types/vendas';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function listMarketplace(
	params: ListMarketplaceParams = {},
	fetchFn: typeof fetch = fetch
): Promise<MarketplaceResult> {
	const query = new URLSearchParams();
	if (params.plataforma) query.set('plataforma', params.plataforma);
	if (params.page !== undefined) query.set('page', String(params.page));
	if (params.pageSize !== undefined) query.set('pageSize', String(params.pageSize));
	const qs = query.toString();
	return await apiFetch<MarketplaceResult>(
		`/api/vendas/marketplace${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function registrarVenda(
	payload: RegistrarVendaPayload,
	fetchFn: typeof fetch = fetch
): Promise<RegistroMarketplace> {
	return await apiFetch<RegistroMarketplace>(
		'/api/vendas/marketplace',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}
