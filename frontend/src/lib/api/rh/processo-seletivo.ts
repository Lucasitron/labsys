import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	CreateGrupoPayload,
	PsEvaluation,
	PsGroup,
	PsListResult,
	PsStage
} from '$lib/types/rh';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function listPS(
	params: { estagio?: PsStage } = {},
	fetchFn: typeof fetch = fetch
): Promise<PsListResult> {
	const query = new URLSearchParams();
	if (params.estagio) query.set('estagio', params.estagio);
	const qs = query.toString();
	return await apiFetch<PsListResult>(
		`/api/rh/processo-seletivo${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function createGrupo(
	payload: CreateGrupoPayload,
	fetchFn: typeof fetch = fetch
): Promise<PsGroup> {
	return await apiFetch<PsGroup>(
		'/api/rh/processo-seletivo/grupos',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function moverEstagio(
	id: string,
	estagio: PsStage,
	fetchFn: typeof fetch = fetch
): Promise<PsGroup> {
	return await apiFetch<PsGroup>(
		`/api/rh/processo-seletivo/${id}/estagio`,
		{ method: 'PATCH', headers: bearer(), body: JSON.stringify({ estagio }) },
		fetchFn
	);
}

export async function avaliarMembro(
	id: string,
	pessoaId: string,
	payload: PsEvaluation,
	fetchFn: typeof fetch = fetch
): Promise<PsEvaluation> {
	return await apiFetch<PsEvaluation>(
		`/api/rh/processo-seletivo/${id}/membros/${pessoaId}/avaliar`,
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}
