import type { Inspecao5S, InspecaoStatus, ItemInspecao5S, Paginado, SetorRef } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, montarQuery } from './client';

const BASE = '/producao/inspecoes-5s';

export type InspecaoFiltros = {
	status?: InspecaoStatus[];
	setor?: string[];
	auditor?: string[];
	page?: number;
	pageSize?: number;
};

export interface CriarInspecaoPayload {
	setor: SetorRef;
	auditorId: string;
	data: string;
}

export interface ConcluirInspecaoPayload {
	itens: ItemInspecao5S[];
}

export interface NomearAuditorPayload {
	auditorId: string;
}

export async function obterInspecoes5S(
	params: InspecaoFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Inspecao5S>> {
	const qs = montarQuery(params);
	return await apiFetch<Paginado<Inspecao5S>>(`${BASE}${qs}`, { headers: bearer() }, fetchFn);
}

export function criarInspecao5S(payload: CriarInspecaoPayload): Promise<Inspecao5S> {
	return apiFetch<Inspecao5S>(BASE, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export async function obterInspecao5SPorId(id: string, fetchFn: typeof fetch = fetch): Promise<Inspecao5S> {
	return await apiFetch<Inspecao5S>(`${BASE}/${id}`, { headers: bearer() }, fetchFn);
}

export function concluirInspecao5S(id: string, payload: ConcluirInspecaoPayload): Promise<Inspecao5S> {
	return apiFetch<Inspecao5S>(`${BASE}/${id}/concluir`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function nomearAuditor5S(id: string, payload: NomearAuditorPayload): Promise<Inspecao5S> {
	return apiFetch<Inspecao5S>(`${BASE}/${id}/nomear-auditor`, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}