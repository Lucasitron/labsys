import type { Paginado, Penalidade5S, PenalidadeStatus, PenalidadeTipo } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, montarQuery } from './client';

const BASE = '/producao/penalidades-5s';

export type PenalidadeFiltros = {
	tipo?: PenalidadeTipo[];
	situacao?: PenalidadeStatus[];
	page?: number;
	pageSize?: number;
};

export interface CriarPenalidadePayload {
	membroId: string;
	tipo: PenalidadeTipo;
	motivo: string;
}

export async function obterPenalidades5S(
	params: PenalidadeFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Penalidade5S>> {
	const qs = montarQuery(params);
	return await apiFetch<Paginado<Penalidade5S>>(`${BASE}${qs}`, { headers: bearer() }, fetchFn);
}

export function criarPenalidade5S(payload: CriarPenalidadePayload): Promise<Penalidade5S> {
	return apiFetch<Penalidade5S>(BASE, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function marcarPenalidade5SCumprida(id: string): Promise<Penalidade5S> {
	return apiFetch<Penalidade5S>(`${BASE}/${id}/cumprida`, {
		method: 'PUT',
		headers: bearer()
	});
}