import type { Paginado, Prioridade, Tarefa, TarefaStatus } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, montarQuery } from './client';

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

export async function obterTarefas(
	params: TarefaFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Tarefa>> {
	const qs = montarQuery(params);
	return await apiFetch<Paginado<Tarefa>>(`${BASE}${qs}`, { headers: bearer() }, fetchFn);
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