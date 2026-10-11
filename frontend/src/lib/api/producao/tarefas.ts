import type { Paginado, Prioridade, Tarefa, TarefaStatus } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, paginarClient, PRIORIDADE_UPPER, STATUS_TAREFA_UPPER, unwrapLista } from './client';

const BASE = '/producao/tarefas';

export type TarefaFiltros = {
	projetoId?: string;
	status?: TarefaStatus[];
	prioridade?: Prioridade[];
	responsavel?: string[];
	page?: number;
	pageSize?: number;
};

export interface CriarTarefaPayload {
	titulo: string;
	descricao?: string;
	projetoId?: string;
	responsavelId: string;
	prioridade: Prioridade;
	prazo: string;
	status: TarefaStatus;
}

export interface MoverTarefaPayload {
	status: TarefaStatus;
}

export interface ApontarHorasTarefaPayload {
	horas: number;
	minutos: number;
	data: string;
}

export interface AtribuirTarefaPayload {
	responsavelId: string;
}

export interface RawTarefa {
	id: number | string;
	idProjeto?: number | string | null;
	titulo: string;
	descricao?: string | null;
	idResponsavel?: number | string | null;
	dataFimPrevista?: string | null;
	status: string;
	prioridade: string;
}

export function mapTarefa(raw: RawTarefa): Tarefa {
	return {
		id: String(raw.id),
		codigo: '',
		titulo: raw.titulo,
		descricao: raw.descricao ?? undefined,
		projeto:
			raw.idProjeto != null
				? { id: String(raw.idProjeto), codigo: '', nome: '' }
				: null,
		responsavel: {
			id: raw.idResponsavel != null ? String(raw.idResponsavel) : '',
			nome: ''
		},
		prioridade: (PRIORIDADE_UPPER[raw.prioridade] ?? 'Média') as Prioridade,
		prazo: raw.dataFimPrevista ?? '',
		status: (STATUS_TAREFA_UPPER[raw.status] ?? 'Pendente') as TarefaStatus
	};
}

export async function obterTarefas(
	params: TarefaFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Tarefa>> {
	const res = await apiFetch<RawTarefa[] | { data: RawTarefa[] }>(BASE, { headers: bearer() }, fetchFn);
	let todas = unwrapLista(res).map(mapTarefa);
	if (params.projetoId) todas = todas.filter((t) => t.projeto?.id === params.projetoId);
	if (params.status && params.status.length > 0) {
		todas = todas.filter((t) => params.status!.includes(t.status));
	}
	if (params.prioridade && params.prioridade.length > 0) {
		todas = todas.filter((t) => params.prioridade!.includes(t.prioridade));
	}
	if (params.responsavel && params.responsavel.length > 0) {
		todas = todas.filter((t) => params.responsavel!.includes(t.responsavel.id));
	}
	return paginarClient(todas, params.page, params.pageSize);
}

export function criarTarefa(payload: CriarTarefaPayload): Promise<Tarefa> {
	return apiFetch<Tarefa>(BASE, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function atualizarTarefa(id: string, payload: Partial<CriarTarefaPayload>): Promise<Tarefa> {
	return apiFetch<Tarefa>(`${BASE}/${id}`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function moverTarefa(id: string, payload: MoverTarefaPayload): Promise<Tarefa> {
	return apiFetch<Tarefa>(`${BASE}/${id}/mover`, {
		method: 'PATCH',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function apontarHorasTarefa(id: string, payload: ApontarHorasTarefaPayload): Promise<Tarefa> {
	return apiFetch<Tarefa>(`${BASE}/${id}/apontar-horas`, {
		method: 'PATCH',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function atribuirTarefa(id: string, payload: AtribuirTarefaPayload): Promise<Tarefa> {
	return apiFetch<Tarefa>(`${BASE}/${id}/atribuir`, {
		method: 'PATCH',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}