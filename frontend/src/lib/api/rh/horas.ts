import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	ExtratoMensalHoras,
	HoraApontamento,
	HoraStatus,
	HorasResult,
	RegistrarHorasPayload,
	RejeitarHorasPayload
} from '$lib/types/rh';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function listHoras(
	params: { status?: HoraStatus; periodo?: string } = {},
	fetchFn: typeof fetch = fetch
): Promise<HorasResult> {
	const query = new URLSearchParams();
	if (params.status) query.set('status', params.status);
	if (params.periodo) query.set('periodo', params.periodo);
	const qs = query.toString();
	return await apiFetch<HorasResult>(
		`/api/rh/horas${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function registrarHoras(
	payload: RegistrarHorasPayload,
	fetchFn: typeof fetch = fetch
): Promise<HoraApontamento> {
	return await apiFetch<HoraApontamento>(
		'/api/rh/horas',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function validarHoras(id: string, fetchFn: typeof fetch = fetch): Promise<HoraApontamento> {
	return await apiFetch<HoraApontamento>(
		`/api/rh/horas/${id}/validar`,
		{ method: 'PATCH', headers: bearer() },
		fetchFn
	);
}

export async function rejeitarHoras(
	id: string,
	payload: RejeitarHorasPayload,
	fetchFn: typeof fetch = fetch
): Promise<HoraApontamento> {
	return await apiFetch<HoraApontamento>(
		`/api/rh/horas/${id}/rejeitar`,
		{ method: 'PATCH', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function getHorasDisponiveis(
	fetchFn: typeof fetch = fetch
): Promise<ExtratoMensalHoras> {
	return await apiFetch<ExtratoMensalHoras>(
		'/api/rh/horas/disponiveis',
		{ headers: bearer() },
		fetchFn
	);
}

export async function listApontamentos(
	params: { status?: HoraStatus; periodo?: string; personId?: string } = {},
	fetchFn: typeof fetch = fetch
): Promise<HorasResult> {
	const query = new URLSearchParams();
	if (params.status) query.set('status', params.status);
	if (params.periodo) query.set('periodo', params.periodo);
	if (params.personId) query.set('personId', params.personId);
	const qs = query.toString();
	return await apiFetch<HorasResult>(
		`/api/rh/apontamentos-horas${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function createApontamento(
	payload: RegistrarHorasPayload,
	fetchFn: typeof fetch = fetch
): Promise<HoraApontamento> {
	return await apiFetch<HoraApontamento>(
		'/api/rh/apontamentos-horas',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function validarApontamento(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<HoraApontamento> {
	return await apiFetch<HoraApontamento>(
		`/api/rh/apontamentos-horas/${id}/validar`,
		{ method: 'PUT', headers: bearer() },
		fetchFn
	);
}
