import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	Cliente,
	ClientesResult,
	CreateClientePayload,
	CreateTagPayload,
	ListClientesParams,
	Tag,
	UpdateClientePayload
} from '$lib/types/vendas';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function listClientes(
	params: ListClientesParams = {},
	fetchFn: typeof fetch = fetch
): Promise<ClientesResult> {
	const query = new URLSearchParams();
	if (params.search) query.set('search', params.search);
	if (params.tipo) query.set('tipo', params.tipo);
	if (params.tags) query.set('tags', params.tags);
	if (params.ordenar) query.set('ordenar', params.ordenar);
	if (params.page !== undefined) query.set('page', String(params.page));
	if (params.pageSize !== undefined) query.set('pageSize', String(params.pageSize));
	const qs = query.toString();
	return await apiFetch<ClientesResult>(
		`/api/vendas/clientes${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function getCliente(id: string, fetchFn: typeof fetch = fetch): Promise<Cliente> {
	return await apiFetch<Cliente>(`/api/vendas/clientes/${id}`, { headers: bearer() }, fetchFn);
}

export async function createCliente(
	payload: CreateClientePayload,
	fetchFn: typeof fetch = fetch
): Promise<Cliente> {
	return await apiFetch<Cliente>(
		'/api/vendas/clientes',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function updateCliente(
	id: string,
	payload: UpdateClientePayload,
	fetchFn: typeof fetch = fetch
): Promise<Cliente> {
	return await apiFetch<Cliente>(
		`/api/vendas/clientes/${id}`,
		{ method: 'PUT', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function deleteCliente(id: string, fetchFn: typeof fetch = fetch): Promise<void> {
	await apiFetch<void>(`/api/vendas/clientes/${id}`, { method: 'DELETE', headers: bearer() }, fetchFn);
}

export async function listTags(fetchFn: typeof fetch = fetch): Promise<Tag[]> {
	return await apiFetch<Tag[]>('/api/vendas/tags', { headers: bearer() }, fetchFn);
}

export async function createTag(
	payload: CreateTagPayload,
	fetchFn: typeof fetch = fetch
): Promise<Tag> {
	return await apiFetch<Tag>(
		'/api/vendas/tags',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function bulkAdicionarTag(
	ids: string[],
	tagId: string,
	fetchFn: typeof fetch = fetch
): Promise<void> {
	await apiFetch<void>(
		'/api/vendas/clientes/bulk-tag',
		{ method: 'POST', headers: bearer(), body: JSON.stringify({ ids, tagId }) },
		fetchFn
	);
}
