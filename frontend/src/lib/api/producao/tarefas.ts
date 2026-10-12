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
	idProjeto: string;
	idResponsavel?: string;
	dataInicio?: string;
	dataFimPrevista?: string;
	prioridade?: Prioridade;
}

export interface MoverTarefaPayload {
	status: TarefaStatus;
	dataConclusao?: string;
}

const TAREFA_STATUS_PARA_UPPER: Record<string, string> = {
	Pendente: 'PENDENTE',
	'Em andamento': 'EM_ANDAMENTO',
	Concluída: 'CONCLUIDA',
	Atrasada: 'ATRASADA'
};

const PRIORIDADE_PARA_UPPER: Record<Prioridade, string> = {
	Baixa: 'BAIXA',
	Média: 'MEDIA',
	Alta: 'ALTA'
};

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
	const idProjeto = Number(payload.idProjeto);
	if (!payload.idProjeto || !Number.isInteger(idProjeto)) {
		throw new Error('Selecione o projeto da tarefa.');
	}
	const body: Record<string, unknown> = {
		idProjeto,
		titulo: payload.titulo
	};
	if (payload.descricao !== undefined) body['descricao'] = payload.descricao;
	if (payload.idResponsavel !== undefined && payload.idResponsavel !== '') {
		body['idResponsavel'] = Number(payload.idResponsavel);
	}
	if (payload.dataInicio) body['dataInicio'] = payload.dataInicio;
	if (payload.dataFimPrevista) body['dataFimPrevista'] = payload.dataFimPrevista;
	if (payload.prioridade !== undefined) body['prioridade'] = PRIORIDADE_PARA_UPPER[payload.prioridade];
	return apiFetch<Tarefa>(BASE, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(body)
	});
}

export function moverTarefa(id: string, payload: MoverTarefaPayload): Promise<Tarefa> {
	const status = TAREFA_STATUS_PARA_UPPER[payload.status];
	if (!status) {
		throw new Error(`Status de tarefa sem par no backend: ${payload.status}.`);
	}
	const body: Record<string, unknown> = { status };
	if (payload.dataConclusao) body['dataConclusao'] = payload.dataConclusao;
	return apiFetch<Tarefa>(`${BASE}/${id}/status`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(body)
	});
}

export function atribuirTarefa(): Promise<never> {
	return Promise.reject(
		new Error('Atribuir tarefa indisponível — sem par no backend (Lote 3).')
	);
}