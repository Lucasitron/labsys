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
	TipoPessoa,
	UpdateClientePayload
} from '$lib/types/vendas';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

interface RawCliente {
	id: number | string;
	tipoPessoa: 'PF' | 'PJ';
	nomeRazaoSocial: string;
	documento: string;
	email: string;
	telefone: string;
	endereco?: string | null;
	tags?: Tag[];
	dataCadastro: string;
	criadoPor?: number | string | null;
	codigo?: string;
	updatedAt?: string;
}

export function mapCliente(raw: RawCliente): Cliente {
	const tipo: TipoPessoa = raw.tipoPessoa === 'PJ' ? 'pj' : 'pf';
	return {
		id: String(raw.id),
		codigo: raw.codigo ?? '',
		tipoPessoa: tipo,
		nome: raw.nomeRazaoSocial,
		documento: raw.documento,
		email: raw.email,
		telefone: raw.telefone,
		endereco: raw.endereco ?? undefined,
		tags: raw.tags ?? [],
		dataCadastro: raw.dataCadastro,
		createdBy: raw.criadoPor != null ? String(raw.criadoPor) : '',
		updatedAt: raw.updatedAt ?? ''
	};
}

function toPayload(payload: CreateClientePayload | UpdateClientePayload): Record<string, unknown> {
	const body: Record<string, unknown> = {};
	if (payload.tipoPessoa !== undefined) {
		body['tipoPessoa'] = payload.tipoPessoa === 'pj' ? 'PJ' : 'PF';
	}
	if (payload.nome !== undefined) body['nomeRazaoSocial'] = payload.nome;
	if (payload.documento !== undefined) body['cpfCnpj'] = payload.documento;
	if (payload.email !== undefined) body['email'] = payload.email;
	if (payload.telefone !== undefined) body['telefone'] = payload.telefone;
	if (payload.endereco !== undefined) body['endereco'] = payload.endereco;
	return body;
}

export async function listClientes(
	params: ListClientesParams = {},
	fetchFn: typeof fetch = fetch
): Promise<ClientesResult> {
	const query = new URLSearchParams();
	if (params.search) query.set('termo', params.search);
	if (params.tipo) query.set('tipo', params.tipo === 'pj' ? 'PJ' : 'PF');
	if (params.tags) query.set('tags', params.tags);
	if (params.ordenar) query.set('ordenar', params.ordenar);
	if (params.page !== undefined) query.set('page', String(params.page));
	if (params.pageSize !== undefined) query.set('pageSize', String(params.pageSize));
	const qs = query.toString();
	const res = await apiFetch<{
		clientes: RawCliente[];
		paginacao: { page: number; pageSize: number; total: number; totalPages: number };
	}>(
		`/vendas/clientes${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
	return {
		clientes: res.clientes.map(mapCliente),
		pagination: {
			page: res.paginacao.page,
			pageSize: res.paginacao.pageSize,
			totalItems: res.paginacao.total,
			totalPages: res.paginacao.totalPages
		}
	};
}

export async function getCliente(id: string, fetchFn: typeof fetch = fetch): Promise<Cliente> {
	const res = await apiFetch<{ cliente: RawCliente }>(
		`/vendas/clientes/${id}`,
		{ headers: bearer() },
		fetchFn
	);
	return mapCliente(res.cliente);
}

export async function createCliente(
	payload: CreateClientePayload,
	fetchFn: typeof fetch = fetch
): Promise<Cliente> {
	const raw = await apiFetch<RawCliente>(
		'/vendas/clientes',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(toPayload(payload)) },
		fetchFn
	);
	return mapCliente(raw);
}

export async function updateCliente(
	id: string,
	payload: UpdateClientePayload,
	fetchFn: typeof fetch = fetch
): Promise<Cliente> {
	const raw = await apiFetch<RawCliente>(
		`/vendas/clientes/${id}`,
		{ method: 'PUT', headers: bearer(), body: JSON.stringify(toPayload(payload)) },
		fetchFn
	);
	return mapCliente(raw);
}

export async function deleteCliente(id: string, fetchFn: typeof fetch = fetch): Promise<void> {
	await apiFetch<void>(`/vendas/clientes/${id}`, { method: 'DELETE', headers: bearer() }, fetchFn);
}

export async function listTags(fetchFn: typeof fetch = fetch): Promise<Tag[]> {
	return await apiFetch<Tag[]>('/vendas/tags', { headers: bearer() }, fetchFn);
}

export async function createTag(
	payload: CreateTagPayload,
	fetchFn: typeof fetch = fetch
): Promise<Tag> {
	return await apiFetch<Tag>(
		'/vendas/tags',
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
		'/vendas/clientes/bulk-tag',
		{
			method: 'POST',
			headers: bearer(),
			body: JSON.stringify({ clienteIds: ids.map(Number), tagId: Number(tagId) })
		},
		fetchFn
	);
}
