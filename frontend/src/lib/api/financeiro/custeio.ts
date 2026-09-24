import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	FechamentoEncomenda,
	CreateFechamentoPayload,
	CustoEncomenda,
	ValorHoraNivel,
	DefinirValorHoraPayload,
	ParametroOverhead,
	DefinirOverheadPayload
} from '$lib/types/financeiro';

const FECHAMENTOS = '/api/financeiro/fechamento-encomenda';
const CUSTOS = '/api/financeiro/custos-encomenda';
const VALORES_HORA = '/api/financeiro/valores-hora';
const OVERHEAD = '/api/financeiro/parametros-overhead';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function listFechamentos(fetchFn: typeof fetch = fetch): Promise<FechamentoEncomenda[]> {
	return await apiFetch<FechamentoEncomenda[]>(FECHAMENTOS, { headers: bearer() }, fetchFn);
}

export async function createFechamento(
	payload: CreateFechamentoPayload,
	fetchFn: typeof fetch = fetch
): Promise<FechamentoEncomenda> {
	return await apiFetch<FechamentoEncomenda>(
		FECHAMENTOS,
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function getCusto(idEncomenda: string, fetchFn: typeof fetch = fetch): Promise<CustoEncomenda> {
	return await apiFetch<CustoEncomenda>(`${CUSTOS}/${idEncomenda}`, { headers: bearer() }, fetchFn);
}

export async function listValoresHora(fetchFn: typeof fetch = fetch): Promise<ValorHoraNivel[]> {
	return await apiFetch<ValorHoraNivel[]>(VALORES_HORA, { headers: bearer() }, fetchFn);
}

export async function definirValorHora(
	payload: DefinirValorHoraPayload,
	fetchFn: typeof fetch = fetch
): Promise<ValorHoraNivel> {
	return await apiFetch<ValorHoraNivel>(
		VALORES_HORA,
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function definirOverhead(
	payload: DefinirOverheadPayload,
	fetchFn: typeof fetch = fetch
): Promise<ParametroOverhead> {
	return await apiFetch<ParametroOverhead>(
		OVERHEAD,
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}
