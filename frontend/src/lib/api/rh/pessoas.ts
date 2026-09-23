import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	CreatePessoaPayload,
	PersonDetail,
	PessoasResult,
	RhListParams,
	UpdatePessoaPayload
} from '$lib/types/rh';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function listPessoas(
	params: RhListParams = {},
	fetchFn: typeof fetch = fetch
): Promise<PessoasResult> {
	const query = new URLSearchParams();
	if (params.search) query.set('search', params.search);
	if (params.setor) query.set('setor', params.setor);
	if (params.nivel) query.set('nivel', params.nivel);
	if (params.status) query.set('status', params.status);
	if (params.page !== undefined) query.set('page', String(params.page));
	if (params.pageSize !== undefined) query.set('pageSize', String(params.pageSize));
	const qs = query.toString();
	return await apiFetch<PessoasResult>(
		`/api/rh/pessoas${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function getPessoa(id: string, fetchFn: typeof fetch = fetch): Promise<PersonDetail> {
	return await apiFetch<PersonDetail>(
		`/api/rh/pessoas/${id}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function createPessoa(
	payload: CreatePessoaPayload,
	fetchFn: typeof fetch = fetch
): Promise<PersonDetail> {
	return await apiFetch<PersonDetail>(
		'/api/rh/pessoas',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function updatePessoa(
	id: string,
	payload: UpdatePessoaPayload,
	fetchFn: typeof fetch = fetch
): Promise<PersonDetail> {
	return await apiFetch<PersonDetail>(
		`/api/rh/pessoas/${id}`,
		{ method: 'PUT', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export { searchPeople } from '$lib/api/rh';
export type { PersonOption } from '$lib/api/rh';
