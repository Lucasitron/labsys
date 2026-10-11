import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	CreateOrcamentoPayload,
	ListOrcamentosParams,
	Orcamento,
	OrcamentoStatus,
	OrcamentosResult,
	UpdateOrcamentoPayload
} from '$lib/types/vendas';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export interface RawOrcamento {
	id: number | string;
	clienteId: number | string;
	clienteNome?: string | null;
	dataCriacao: string;
	validade?: string | null;
	valorTotal: number | string;
	status: OrcamentoStatus;
	criadoPor?: number | string | null;
	codigo?: string;
	qtdItens?: number;
}

export function mapOrcamento(raw: RawOrcamento): Orcamento {
	return {
		id: String(raw.id),
		...(raw.codigo ? { codigo: raw.codigo } : {}),
		cliente: { id: String(raw.clienteId), nome: raw.clienteNome ?? '' },
		valorTotal: Number(raw.valorTotal),
		status: raw.status,
		validade: raw.validade ?? '',
		...(raw.qtdItens !== undefined ? { qtdItens: raw.qtdItens } : {}),
		createdBy: raw.criadoPor != null ? String(raw.criadoPor) : ''
	};
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
	const res = await apiFetch<{ orcamentos: RawOrcamento[]; counts: Record<string, number> }>(
		`/vendas/orcamentos${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
	return { orcamentos: res.orcamentos.map(mapOrcamento), counts: res.counts };
}

export async function getOrcamento(id: string, fetchFn: typeof fetch = fetch): Promise<Orcamento> {
	const raw = await apiFetch<RawOrcamento>(
		`/vendas/orcamentos/${id}`,
		{ headers: bearer() },
		fetchFn
	);
	return mapOrcamento(raw);
}

export async function createOrcamento(
	payload: CreateOrcamentoPayload,
	fetchFn: typeof fetch = fetch
): Promise<Orcamento> {
	const raw = await apiFetch<RawOrcamento>(
		'/vendas/orcamentos',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
	return mapOrcamento(raw);
}

export async function updateOrcamento(
	id: string,
	payload: UpdateOrcamentoPayload,
	fetchFn: typeof fetch = fetch
): Promise<Orcamento> {
	const raw = await apiFetch<RawOrcamento>(
		`/vendas/orcamentos/${id}`,
		{ method: 'PUT', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
	return mapOrcamento(raw);
}
