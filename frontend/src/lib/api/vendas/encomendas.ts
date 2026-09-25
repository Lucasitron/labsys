import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	CreateEncomendaPayload,
	Encomenda,
	EncomendasResult,
	HistoricoEncomenda,
	ListEncomendasParams,
	MoverKanbanPayload
} from '$lib/types/vendas';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function listEncomendas(
	params: ListEncomendasParams = {},
	fetchFn: typeof fetch = fetch
): Promise<EncomendasResult> {
	const query = new URLSearchParams();
	if (params.status_kanban) query.set('status_kanban', params.status_kanban);
	if (params.search) query.set('search', params.search);
	if (params.clienteId) query.set('clienteId', params.clienteId);
	const qs = query.toString();
	return await apiFetch<EncomendasResult>(
		`/api/vendas/encomendas${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function getEncomenda(id: string, fetchFn: typeof fetch = fetch): Promise<Encomenda> {
	return await apiFetch<Encomenda>(
		`/api/vendas/encomendas/${id}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function createEncomenda(
	payload: CreateEncomendaPayload,
	fetchFn: typeof fetch = fetch
): Promise<Encomenda> {
	return await apiFetch<Encomenda>(
		'/api/vendas/encomendas',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function moverKanban(
	id: string,
	payload: MoverKanbanPayload,
	fetchFn: typeof fetch = fetch
): Promise<HistoricoEncomenda> {
	return await apiFetch<HistoricoEncomenda>(
		`/api/vendas/encomendas/${id}/kanban`,
		{ method: 'PUT', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}
