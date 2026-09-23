import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	CreateOrcamentoPayload,
	ListOrcamentosParams,
	Orcamento,
	OrcamentosResult,
	UpdateOrcamentoPayload
} from '$lib/types/vendas';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function listOrcamentos(
	params: ListOrcamentosParams = {},
	fetchFn: typeof fetch = fetch
): Promise<OrcamentosResult> {
	const query = new URLSearchParams();
	if (params.status) query.set('status', params.status);
	if (params.search) query.set('search', params.search);
	if (params.clienteId) query.set('clienteId', params.clienteId);
	if (params.page !== undefined) query.set('page', String(params.page));
	if (params.pageSize !== undefined) query.set('pageSize', String(params.pageSize));
	const qs = query.toString();
	return await apiFetch<OrcamentosResult>(
		`/api/vendas/orcamentos${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function getOrcamento(id: string, fetchFn: typeof fetch = fetch): Promise<Orcamento> {
	return await apiFetch<Orcamento>(
		`/api/vendas/orcamentos/${id}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function createOrcamento(
	payload: CreateOrcamentoPayload,
	fetchFn: typeof fetch = fetch
): Promise<Orcamento> {
	return await apiFetch<Orcamento>(
		'/api/vendas/orcamentos',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function updateOrcamento(
	id: string,
	payload: UpdateOrcamentoPayload,
	fetchFn: typeof fetch = fetch
): Promise<Orcamento> {
	return await apiFetch<Orcamento>(
		`/api/vendas/orcamentos/${id}`,
		{ method: 'PUT', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}
