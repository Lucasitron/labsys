import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	FluxoCaixa,
	Dre,
	Lucratividade,
	Inadimplencia,
	DoacoesDespesas,
	CustoMaquina,
	RelatorioPeriodoParams
} from '$lib/types/financeiro';

const BASE = '/api/financeiro/relatorios';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

function periodoQuery(params: RelatorioPeriodoParams = {}): string {
	const query = new URLSearchParams();
	if (params.periodo) query.set('periodo', params.periodo);
	const qs = query.toString();
	return qs ? `?${qs}` : '';
}

export async function getFluxoCaixa(
	params: RelatorioPeriodoParams = {},
	fetchFn: typeof fetch = fetch
): Promise<FluxoCaixa> {
	return await apiFetch<FluxoCaixa>(
		`${BASE}/fluxo-caixa${periodoQuery(params)}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function getDre(
	params: RelatorioPeriodoParams = {},
	fetchFn: typeof fetch = fetch
): Promise<Dre> {
	return await apiFetch<Dre>(`${BASE}/dre${periodoQuery(params)}`, { headers: bearer() }, fetchFn);
}

export async function getLucratividade(
	params: RelatorioPeriodoParams = {},
	fetchFn: typeof fetch = fetch
): Promise<Lucratividade> {
	return await apiFetch<Lucratividade>(
		`${BASE}/lucratividade${periodoQuery(params)}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function getInadimplencia(
	params: RelatorioPeriodoParams = {},
	fetchFn: typeof fetch = fetch
): Promise<Inadimplencia> {
	return await apiFetch<Inadimplencia>(
		`${BASE}/inadimplencia${periodoQuery(params)}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function getDoacoesDespesas(
	params: RelatorioPeriodoParams = {},
	fetchFn: typeof fetch = fetch
): Promise<DoacoesDespesas> {
	return await apiFetch<DoacoesDespesas>(
		`${BASE}/doacoes-despesas${periodoQuery(params)}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function getCustoMaquina(
	params: RelatorioPeriodoParams = {},
	fetchFn: typeof fetch = fetch
): Promise<CustoMaquina> {
	return await apiFetch<CustoMaquina>(
		`${BASE}/custo-maquina${periodoQuery(params)}`,
		{ headers: bearer() },
		fetchFn
	);
}
