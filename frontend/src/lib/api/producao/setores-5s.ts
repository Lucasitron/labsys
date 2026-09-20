import type { Ciclo5S, Paginado, Setor5S } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, montarQuery } from './client';

const BASE = '/producao/5s/setores';

export type Setor5SFiltros = {
	ciclo?: Ciclo5S[];
	page?: number;
	pageSize?: number;
};

export interface ResponsavelSetorPayload {
	membroId: string;
	ps: boolean;
}

export interface CriarSetor5SPayload {
	nome: string;
	ciclo: Ciclo5S;
	responsaveis: ResponsavelSetorPayload[];
	auditorId: string;
}

export interface AtualizarChecklistPayload {
	itens: string[];
}

export interface AprovarAlteracaoPayload {
	aprovado: boolean;
	observacao?: string;
}

export async function obterSetores5S(
	params: Setor5SFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Setor5S>> {
	const qs = montarQuery(params);
	return await apiFetch<Paginado<Setor5S>>(`${BASE}${qs}`, { headers: bearer() }, fetchFn);
}

export async function obterSetor5SPorId(id: string, fetchFn: typeof fetch = fetch): Promise<Setor5S> {
	return await apiFetch<Setor5S>(`${BASE}/${id}`, { headers: bearer() }, fetchFn);
}

export function criarSetor5S(payload: CriarSetor5SPayload): Promise<Setor5S> {
	return apiFetch<Setor5S>(BASE, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function atualizarSetor5S(id: string, payload: Partial<CriarSetor5SPayload>): Promise<Setor5S> {
	return apiFetch<Setor5S>(`${BASE}/${id}`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function atualizarResponsaveisDoSetor(id: string, payload: { responsaveis: ResponsavelSetorPayload[] }): Promise<Setor5S> {
	return apiFetch<Setor5S>(`${BASE}/${id}/responsaveis`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function atualizarChecklistDoSetor(id: string, payload: AtualizarChecklistPayload): Promise<Setor5S> {
	return apiFetch<Setor5S>(`${BASE}/${id}/checklist`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function aprovarAlteracaoDoSetor(id: string, payload: AprovarAlteracaoPayload): Promise<Setor5S> {
	return apiFetch<Setor5S>(`${BASE}/${id}/aprovar-alteracao`, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}