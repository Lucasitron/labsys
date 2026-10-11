import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
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
		`/rh/treinamentos${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function getTreinamento(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<TreinamentoDetail> {
	return await apiFetch<TreinamentoDetail>(
		`/rh/treinamentos/${id}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function avaliarTreinamento(
	id: string,
	payload: AvaliarTreinamentoPayload,
	fetchFn: typeof fetch = fetch
): Promise<void> {
	let idFuncionario = payload.idFuncionario ? Number(payload.idFuncionario) : NaN;
	if (!Number.isInteger(idFuncionario) && payload.pessoaId) {
		const detalhe = await apiFetch<{ idFuncionario?: number | string | null }>(
			`/rh/pessoas/${payload.pessoaId}/detalhe`,
			{ headers: bearer() },
			fetchFn
		);
		idFuncionario = Number(detalhe.idFuncionario);
	}
	if (!Number.isInteger(idFuncionario)) {
		throw new Error('Selecione o funcionário avaliado.');
	}
	await apiFetch<void>(
		`/rh/treinamentos/${id}/avaliacoes`,
		{
			method: 'POST',
			headers: bearer(),
			body: JSON.stringify({
				idFuncionario,
				nota: Number(payload.nota),
				...(payload.feedback?.trim() ? { feedback: payload.feedback.trim() } : {})
			})
		},
		fetchFn
	);
}

// Sem par no backend (F35, Lote 3 do back): gateadas com mensagem.
export async function getSessao(): Promise<TreinamentoSessao> {
	throw new Error('Sessão de treinamento indisponível — sem par no backend (Lote 3).');
}

export async function atribuirTarefaFinal(): Promise<FinalTask> {
	throw new Error('Tarefa final indisponível — sem par no backend (Lote 3).');
}

export async function listAgenda(): Promise<TreinamentosResult> {
	throw new Error('Agenda de treinamentos indisponível — sem par no backend (Lote 3).');
}

export async function getDisponibilidade(): Promise<InstrutorDisponibilidade[]> {
	throw new Error('Disponibilidade de instrutores indisponível — sem par no backend (Lote 3).');
}

export async function listGuias(): Promise<GuiasResult> {
	throw new Error('Guias indisponíveis — sem par no backend (Lote 3).');
}
