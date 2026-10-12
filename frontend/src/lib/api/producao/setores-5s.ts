import type { Ciclo5S, Paginado, Setor5S } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, paginarClient, unwrapLista } from './client';

const BASE = '/producao/setores';

export type Setor5SFiltros = {
	ciclo?: Ciclo5S[];
	page?: number;
	pageSize?: number;
};

export interface CriarSetor5SPayload {
	numero: number;
	nome: string;
	descricao?: string;
	observacoes?: string;
	fotoCorretoUrl?: string;
	fotoIncorretoUrl?: string;
	ativo?: boolean;
}

export interface AdicionarChecklistPayload {
	item: string;
	ativo?: boolean;
}

export interface AtualizarChecklistItemPayload {
	item: string;
	ativo?: boolean;
}

export interface AdicionarResponsavelPayload {
	idFuncionario: string;
	dataInicio: string;
	dataFim?: string;
	ativo?: boolean;
}

export interface AdicionarMaterialPayload {
	idItem: string;
	quantidade?: number;
}

export interface AdicionarSinalizacaoPayload {
	tipo: string;
	descricao?: string;
}

export interface RawSetor {
	id: number | string;
	numero?: number | string | null;
	nome: string;
	descricao?: string | null;
	responsaveis?: { idFuncionario?: number | string | null; id?: number | string | null }[] | null;
	checklists?: { id?: number | string | null; item?: string | null }[] | null;
}

// Adaptador de leitura: o back não serve ciclo/cartão/auditor/nota —
// exibição com neutros (sem inventar vínculo); checklist conta via itens.
export function mapSetor(raw: RawSetor): Setor5S {
	const responsaveis = (raw.responsaveis ?? []).map((r) => ({
		membro: { id: String(r.idFuncionario ?? r.id ?? ''), nome: '' },
		ps: false
	}));
	return {
		id: String(raw.id),
		nome: raw.nome,
		ciclo: 'mensal',
		responsaveis,
		auditor: { id: '', nome: '' },
		nota: null,
		cartao: 'Amarelo',
		ultimaAuditoria: null,
		proximaAuditoria: ''
	};
}

export async function obterSetores5S(
	params: Setor5SFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Setor5S>> {
	const res = await apiFetch<RawSetor[] | { data: RawSetor[] }>(BASE, { headers: bearer() }, fetchFn);
	const todos = unwrapLista(res).map(mapSetor);
	if (params.ciclo && params.ciclo.length > 0) {
		return paginarClient(
			todos.filter((s) => params.ciclo!.includes(s.ciclo)),
			params.page,
			params.pageSize
		);
	}
	return paginarClient(todos, params.page, params.pageSize);
}

export async function obterSetor5SPorId(id: string, fetchFn: typeof fetch = fetch): Promise<Setor5S> {
	const raw = await apiFetch<RawSetor>(`${BASE}/${id}`, { headers: bearer() }, fetchFn);
	return mapSetor(raw);
}

export function criarSetor5S(payload: CriarSetor5SPayload): Promise<Setor5S> {
	const body: Record<string, unknown> = {
		numero: Number(payload.numero),
		nome: payload.nome
	};
	if (payload.descricao !== undefined) body['descricao'] = payload.descricao;
	if (payload.observacoes !== undefined) body['observacoes'] = payload.observacoes;
	if (payload.fotoCorretoUrl !== undefined) body['fotoCorretoUrl'] = payload.fotoCorretoUrl;
	if (payload.fotoIncorretoUrl !== undefined) body['fotoIncorretoUrl'] = payload.fotoIncorretoUrl;
	if (payload.ativo !== undefined) body['ativo'] = payload.ativo;
	return apiFetch<Setor5S>(BASE, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(body)
	});
}

export function atualizarSetor5S(id: string, payload: Partial<CriarSetor5SPayload>): Promise<Setor5S> {
	return apiFetch<Setor5S>(`${BASE}/${id}`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function adicionarChecklistDoSetor(id: string, payload: AdicionarChecklistPayload): Promise<Setor5S> {
	return apiFetch<Setor5S>(`${BASE}/${id}/checklist`, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function atualizarChecklistDoSetor(
	id: string,
	idItem: string,
	payload: AtualizarChecklistItemPayload
): Promise<Setor5S> {
	return apiFetch<Setor5S>(`${BASE}/${id}/checklist/${encodeURIComponent(idItem)}`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function removerChecklistDoSetor(id: string, idItem: string): Promise<void> {
	return apiFetch<void>(`${BASE}/${id}/checklist/${encodeURIComponent(idItem)}`, {
		method: 'DELETE',
		headers: bearer()
	});
}

export function adicionarResponsavelDoSetor(
	id: string,
	payload: AdicionarResponsavelPayload
): Promise<Setor5S> {
	const idFuncionario = Number(payload.idFuncionario);
	if (!Number.isInteger(idFuncionario)) {
		throw new Error('Informe o funcionário responsável pelo setor.');
	}
	if (!payload.dataInicio) {
		throw new Error('Informe a data de início da responsabilidade.');
	}
	return apiFetch<Setor5S>(`${BASE}/${id}/responsaveis`, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify({ ...payload, idFuncionario })
	});
}

export function adicionarMaterialDoSetor(id: string, payload: AdicionarMaterialPayload): Promise<Setor5S> {
	return apiFetch<Setor5S>(`${BASE}/${id}/materiais`, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify({ ...payload, idItem: Number(payload.idItem) })
	});
}

export function adicionarSinalizacaoDoSetor(
	id: string,
	payload: AdicionarSinalizacaoPayload
): Promise<Setor5S> {
	return apiFetch<Setor5S>(`${BASE}/${id}/sinalizacoes`, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

// Sem par no backend (F43): gateada com mensagem, sem mapear no vazio.
export function aprovarAlteracaoDoSetor(): Promise<never> {
	return Promise.reject(
		new Error('Aprovação de alteração indisponível — sem par no backend (Lote 3).')
	);
}

// Legado sem par granular 1:1 (substituído pelas fns por item acima).
export function atualizarResponsaveisDoSetor(): Promise<never> {
	return Promise.reject(
		new Error('Use a designação de responsável por período (sem par em lote no backend).')
	);
}