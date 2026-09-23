import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	AtribuirTarefaFinalPayload,
	AvaliarTreinamentoPayload,
	FinalTask,
	GuiasResult,
	InstrutorDisponibilidade,
	TreinamentoDetail,
	TreinamentoSessao,
	TreinamentosResult,
	TrainingStatus
} from '$lib/types/rh';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function listTreinamentos(
	params: { status?: TrainingStatus; search?: string } = {},
	fetchFn: typeof fetch = fetch
): Promise<TreinamentosResult> {
	const query = new URLSearchParams();
	if (params.status) query.set('status', params.status);
	if (params.search) query.set('search', params.search);
	const qs = query.toString();
	return await apiFetch<TreinamentosResult>(
		`/api/rh/treinamentos${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function getTreinamento(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<TreinamentoDetail> {
	return await apiFetch<TreinamentoDetail>(
		`/api/rh/treinamentos/${id}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function getSessao(id: string, fetchFn: typeof fetch = fetch): Promise<TreinamentoSessao> {
	return await apiFetch<TreinamentoSessao>(
		`/api/rh/treinamentos/${id}/sessao`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function avaliarTreinamento(
	id: string,
	payload: AvaliarTreinamentoPayload,
	fetchFn: typeof fetch = fetch
): Promise<void> {
	await apiFetch<void>(
		`/api/rh/treinamentos/${id}/avaliar`,
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function atribuirTarefaFinal(
	id: string,
	payload: AtribuirTarefaFinalPayload,
	fetchFn: typeof fetch = fetch
): Promise<FinalTask> {
	return await apiFetch<FinalTask>(
		`/api/rh/treinamentos/${id}/tarefa-final`,
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function listAgenda(
	params: { tab?: string } = {},
	fetchFn: typeof fetch = fetch
): Promise<TreinamentosResult> {
	const query = new URLSearchParams();
	if (params.tab) query.set('tab', params.tab);
	const qs = query.toString();
	return await apiFetch<TreinamentosResult>(
		`/api/rh/treinamentos/agenda${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function getDisponibilidade(
	fetchFn: typeof fetch = fetch
): Promise<InstrutorDisponibilidade[]> {
	return await apiFetch<InstrutorDisponibilidade[]>(
		'/api/rh/treinamentos/disponibilidade',
		{ headers: bearer() },
		fetchFn
	);
}

export async function listGuias(
	params: { maquinaId?: string } = {},
	fetchFn: typeof fetch = fetch
): Promise<GuiasResult> {
	const query = new URLSearchParams();
	if (params.maquinaId) query.set('maquinaId', params.maquinaId);
	const qs = query.toString();
	return await apiFetch<GuiasResult>(
		`/api/rh/treinamentos/guias${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
}
