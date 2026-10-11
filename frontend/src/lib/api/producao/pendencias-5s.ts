import type { GravidadePendencia, Paginado, Pendencia5S, PenalidadeTipo, SituacaoPendencia } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, paraPaginado } from './client';

const BASE = '/producao/pendencias-5s';

export type PendenciaFiltros = {
	gravidade?: GravidadePendencia[];
	setor?: string[];
	situacao?: SituacaoPendencia[];
	page?: number;
	pageSize?: number;
};

export interface ResolverPendenciaPayload {
	observacao?: string;
}

export interface RegistrarPenalidadePayload {
	tipo: PenalidadeTipo;
	motivo: string;
}

export async function obterPendencias5S(
	params: PendenciaFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Pendencia5S>> {
	const res = await apiFetch<Paginado<Pendencia5S> | Pendencia5S[] | { data: Pendencia5S[] }>(BASE, { headers: bearer() }, fetchFn);
	return paraPaginado(res, params.page, params.pageSize);
}

export async function obterPendencia5SPorId(id: string, fetchFn: typeof fetch = fetch): Promise<Pendencia5S> {
	return await apiFetch<Pendencia5S>(`${BASE}/${id}`, { headers: bearer() }, fetchFn);
}

export function resolverPendencia5S(id: string, payload: ResolverPendenciaPayload): Promise<Pendencia5S> {
	return apiFetch<Pendencia5S>(`${BASE}/${id}/resolver`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function registrarPenalidadeParaPendencia5S(
	id: string,
	payload: RegistrarPenalidadePayload
): Promise<Pendencia5S> {
	return apiFetch<Pendencia5S>(`${BASE}/${id}/penalidade`, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}