import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	CreateTarefaPayload,
	ListTarefasParams,
	Prioridade,
	TarefaMarketing,
	TarefasResult,
	TarefaStatus,
	UpdateTarefaPayload
} from '$lib/types/vendas';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

// O back (TarefaResource) devolve `criadoPor` (sem `createdBy`): ler com alias.
export interface RawTarefaMarketing {
	id: number | string;
	titulo: string;
	descricao?: string | null;
	responsavelId: number | string;
	dataInicio?: string | null;
	dataFim?: string | null;
	status: TarefaStatus;
	prioridade: Prioridade;
	criadoPor?: number | string | null;
	createdBy?: number | string | null;
}

export function mapTarefaMarketing(raw: RawTarefaMarketing): TarefaMarketing {
	return {
		id: String(raw.id),
		titulo: raw.titulo,
		...(raw.descricao ? { descricao: raw.descricao } : {}),
		responsavelId: String(raw.responsavelId),
		...(raw.dataInicio ? { dataInicio: raw.dataInicio } : {}),
		...(raw.dataFim ? { dataFim: raw.dataFim } : {}),
		status: raw.status,
		prioridade: raw.prioridade,
		createdBy:
			raw.criadoPor != null
				? String(raw.criadoPor)
				: (raw.createdBy != null ? String(raw.createdBy) : '')
	};
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
	const res = await apiFetch<{ tarefas: RawTarefaMarketing[]; counts: Record<string, number> }>(
		`/vendas/tarefas-marketing${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
	return { tarefas: (res.tarefas ?? []).map(mapTarefaMarketing), counts: res.counts };
}

export async function createTarefa(
	payload: CreateTarefaPayload,
	fetchFn: typeof fetch = fetch
): Promise<TarefaMarketing> {
	const raw = await apiFetch<RawTarefaMarketing>(
		'/vendas/tarefas-marketing',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
	return mapTarefaMarketing(raw);
}

export async function updateTarefa(
	id: string,
	payload: UpdateTarefaPayload,
	fetchFn: typeof fetch = fetch
): Promise<TarefaMarketing> {
	const raw = await apiFetch<RawTarefaMarketing>(
		`/vendas/tarefas-marketing/${id}`,
		{ method: 'PUT', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
	return mapTarefaMarketing(raw);
}
