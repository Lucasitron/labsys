import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	DoacaoRecurso,
	DoacoesResult,
	CreateDoacaoPayload,
	ListDoacoesParams
} from '$lib/types/financeiro';

const BASE = '/api/financeiro/doacoes-recursos';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function listDoacoes(
	params: ListDoacoesParams = {},
	fetchFn: typeof fetch = fetch
): Promise<DoacoesResult> {
	const query = new URLSearchParams();
	if (params.search) query.set('search', params.search);
	if (params.tipo) query.set('tipo', params.tipo);
	if (params.periodo) query.set('periodo', params.periodo);
	if (params.page !== undefined) query.set('page', String(params.page));
	const qs = query.toString();
	return await apiFetch<DoacoesResult>(`${BASE}${qs ? `?${qs}` : ''}`, { headers: bearer() }, fetchFn);
}

export async function createDoacao(
	payload: CreateDoacaoPayload,
	fetchFn: typeof fetch = fetch
): Promise<DoacaoRecurso> {
	return await apiFetch<DoacaoRecurso>(
		BASE,
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}
