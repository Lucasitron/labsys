import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	CreateTarefaPayload,
	ListTarefasParams,
	TarefaMarketing,
	TarefasResult,
	UpdateTarefaPayload
} from '$lib/types/vendas';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function listTarefas(
	params: ListTarefasParams = {},
	fetchFn: typeof fetch = fetch
): Promise<TarefasResult> {
	const query = new URLSearchParams();
	if (params.search) query.set('search', params.search);
	if (params.status) query.set('status', params.status);
	if (params.prioridade) query.set('prioridade', params.prioridade);
	if (params.page !== undefined) query.set('page', String(params.page));
	if (params.pageSize !== undefined) query.set('pageSize', String(params.pageSize));
	const qs = query.toString();
	return await apiFetch<TarefasResult>(
		`/api/vendas/tarefas-marketing${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function createTarefa(
	payload: CreateTarefaPayload,
	fetchFn: typeof fetch = fetch
): Promise<TarefaMarketing> {
	return await apiFetch<TarefaMarketing>(
		'/api/vendas/tarefas-marketing',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function updateTarefa(
	id: string,
	payload: UpdateTarefaPayload,
	fetchFn: typeof fetch = fetch
): Promise<TarefaMarketing> {
	return await apiFetch<TarefaMarketing>(
		`/api/vendas/tarefas-marketing/${id}`,
		{ method: 'PUT', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}
