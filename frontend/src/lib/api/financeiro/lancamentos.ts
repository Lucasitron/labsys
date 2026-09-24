import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	Lancamento,
	LancamentosResult,
	CreateLancamentoPayload,
	RegistrarPagamentoPayload,
	ListLancamentosParams
} from '$lib/types/financeiro';

const BASE = '/api/financeiro/lancamentos';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function listLancamentos(
	params: ListLancamentosParams = {},
	fetchFn: typeof fetch = fetch
): Promise<LancamentosResult> {
	const query = new URLSearchParams();
	if (params.search) query.set('search', params.search);
	if (params.status) query.set('status', params.status);
	if (params.tipo) query.set('tipo', params.tipo);
	if (params.categoria) query.set('categoria', params.categoria);
	if (params.periodo) query.set('periodo', params.periodo);
	if (params.ordenar) query.set('ordenar', params.ordenar);
	if (params.page !== undefined) query.set('page', String(params.page));
	if (params.pageSize !== undefined) query.set('pageSize', String(params.pageSize));
	const qs = query.toString();
	return await apiFetch<LancamentosResult>(`${BASE}${qs ? `?${qs}` : ''}`, { headers: bearer() }, fetchFn);
}

export async function getLancamento(id: string, fetchFn: typeof fetch = fetch): Promise<Lancamento> {
	return await apiFetch<Lancamento>(`${BASE}/${id}`, { headers: bearer() }, fetchFn);
}

export async function createLancamento(
	payload: CreateLancamentoPayload,
	fetchFn: typeof fetch = fetch
): Promise<Lancamento> {
	return await apiFetch<Lancamento>(
		BASE,
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function registrarPagamento(
	id: string,
	payload: RegistrarPagamentoPayload,
	fetchFn: typeof fetch = fetch
): Promise<Lancamento> {
	return await apiFetch<Lancamento>(
		`${BASE}/${id}/pagamento`,
		{ method: 'PUT', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}
