import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	CreateEncomendaPayload,
	Encomenda,
	EncomendasResult,
	KanbanStatus,
	ListEncomendasParams,
	MoverKanbanPayload
} from '$lib/types/vendas';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export interface RawEncomenda {
	id: number | string;
	idOrcamento?: number | string | null;
	idCliente: number | string;
	clienteNome?: string | null;
	dataCriacao: string;
	dataPrevisaoEntrega?: string | null;
	statusKanban: KanbanStatus;
	valorFinal: number | string;
	versao?: number | string | null;
	criadoPor?: number | string | null;
	codigo?: string;
	itensCount?: number;
}

export function mapEncomenda(raw: RawEncomenda): Encomenda {
	return {
		id: String(raw.id),
		...(raw.codigo ? { codigo: raw.codigo } : {}),
		cliente: { id: String(raw.idCliente), nome: raw.clienteNome ?? '' },
		...(raw.clienteNome ? { clienteNome: raw.clienteNome } : {}),
		statusKanban: raw.statusKanban,
		origem: 'Venda direta',
		previsao: raw.dataPrevisaoEntrega ?? '',
		valorFinal: Number(raw.valorFinal),
		...(raw.itensCount !== undefined ? { itensCount: raw.itensCount } : {}),
		createdBy: raw.criadoPor != null ? String(raw.criadoPor) : '',
		versao: raw.versao != null ? Number(raw.versao) : 0
	};
}

export async function listEncomendas(
	params: ListEncomendasParams = {},
	fetchFn: typeof fetch = fetch
): Promise<EncomendasResult> {
	const query = new URLSearchParams();
	if (params.status) query.set('status', params.status);
	if (params.search) query.set('search', params.search);
	if (params.clienteId) query.set('clienteId', params.clienteId);
	const qs = query.toString();
	const res = await apiFetch<{ encomendas: RawEncomenda[]; counts: Record<string, number> }>(
		`/vendas/encomendas${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
	return { encomendas: res.encomendas.map(mapEncomenda), counts: res.counts };
}

export async function getEncomenda(id: string, fetchFn: typeof fetch = fetch): Promise<Encomenda> {
	const raw = await apiFetch<RawEncomenda>(
		`/vendas/encomendas/${id}`,
		{ headers: bearer() },
		fetchFn
	);
	return mapEncomenda(raw);
}

export async function createEncomenda(
	payload: CreateEncomendaPayload,
	fetchFn: typeof fetch = fetch
): Promise<Encomenda> {
	const body: Record<string, unknown> = {};
	if (payload.idOrcamento !== undefined) body['idOrcamento'] = payload.idOrcamento;
	if (payload.clienteId !== undefined) body['clienteId'] = payload.clienteId;
	if (payload.valorFinal !== undefined) body['valorFinal'] = payload.valorFinal;
	if (payload.previsao !== undefined) body['dataPrevisaoEntrega'] = payload.previsao;
	if (payload.observacoes !== undefined) body['observacoes'] = payload.observacoes;
	const raw = await apiFetch<RawEncomenda>(
		'/vendas/encomendas',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(body) },
		fetchFn
	);
	return mapEncomenda(raw);
}

export async function moverKanban(
	id: string,
	payload: MoverKanbanPayload,
	fetchFn: typeof fetch = fetch
): Promise<Encomenda> {
	const raw = await apiFetch<RawEncomenda>(
		`/vendas/encomendas/${id}/kanban`,
		{
			method: 'PUT',
			headers: bearer(),
			body: JSON.stringify({
				statusKanban: payload.statusKanban,
				versao: payload.versao,
				...(payload.observacao ? { observacao: payload.observacao } : {})
			})
		},
		fetchFn
	);
	return mapEncomenda(raw);
}
