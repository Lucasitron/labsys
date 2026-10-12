import { bearer } from '$lib/api/bearer';
import { apiFetch } from '$lib/api/client';
import type {
	CreateEncomendaPayload,
	Encomenda,
	EncomendasResult,
	KanbanStatus,
	ListEncomendasParams,
	MoverKanbanPayload
} from '$lib/types/vendas';


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
	// O back lê só `status` (Title-case) + `clienteId`; `search` filtra client-side.
	const query = new URLSearchParams();
	if (params.status) query.set('status', params.status);
	if (params.clienteId) query.set('clienteId', params.clienteId);
	const qs = query.toString();
	const res = await apiFetch<{ encomendas: RawEncomenda[]; counts: Record<string, number> }>(
		`/vendas/encomendas${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
	let encomendas = res.encomendas.map(mapEncomenda);
	const termo = params.search?.trim().toLowerCase();
	if (termo) {
		encomendas = encomendas.filter(
			(e) =>
				e.cliente.nome.toLowerCase().includes(termo) ||
				(e.codigo ?? '').toLowerCase().includes(termo)
		);
	}
	return { encomendas, counts: res.counts };
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
	if (payload.idOrcamento !== undefined) body['idOrcamento'] = Number(payload.idOrcamento);
	if (payload.clienteId !== undefined) body['clienteId'] = Number(payload.clienteId);
	if (payload.valorFinal !== undefined) body['valorFinal'] = Number(payload.valorFinal);
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

export interface NovaOrdemPayload {
	valorFinal: number;
	dataPrevisaoEntrega?: string;
	observacoes?: string;
}

export async function novaOrdem(
	id: string,
	payload: NovaOrdemPayload,
	fetchFn: typeof fetch = fetch
): Promise<Encomenda> {
	const raw = await apiFetch<RawEncomenda>(
		`/vendas/encomendas/${id}/nova-ordem`,
		{
			method: 'POST',
			headers: bearer(),
			body: JSON.stringify({
				valorFinal: Number(payload.valorFinal),
				...(payload.dataPrevisaoEntrega ? { dataPrevisaoEntrega: payload.dataPrevisaoEntrega } : {}),
				...(payload.observacoes ? { observacoes: payload.observacoes } : {})
			})
		},
		fetchFn
	);
	return mapEncomenda(raw);
}

export interface RawHistorico {
	statusAnterior: string;
	statusNovo: string;
	dataAlteracao: string;
	idUsuario?: number | string | null;
	responsavel?: string | null;
	observacao?: string | null;
}

export async function getHistoricoStatus(
	idEncomenda: string,
	fetchFn: typeof fetch = fetch
): Promise<import('$lib/types/vendas').HistoricoEncomenda[]> {
	const raw = await apiFetch<RawHistorico[]>(
		`/vendas/historico-status/${idEncomenda}`,
		{ headers: bearer() },
		fetchFn
	);
	return (raw ?? []).map((h) => ({
		statusAnterior: h.statusAnterior as import('$lib/types/vendas').KanbanStatus,
		statusNovo: h.statusNovo as import('$lib/types/vendas').KanbanStatus,
		dataAlteracao: h.dataAlteracao,
		responsavel: h.responsavel ?? (h.idUsuario != null ? String(h.idUsuario) : ''),
		...(h.observacao ? { observacao: h.observacao } : {})
	}));
}
