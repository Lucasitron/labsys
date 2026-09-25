import type { Mesa5S, MesaAuditoriaResultado, Paginado, SituacaoMesas } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, montarQuery } from './client';

const BASE = '/producao/mesas';

export type MesaFiltros = {
	mes?: string;
	situacao?: SituacaoMesas[];
	search?: string;
	page?: number;
	pageSize?: number;
};

export interface CriarMesaPayload {
	nome: string;
	projetoId?: string;
	membroId: string;
	periodoExperimental?: boolean;
}

export interface EvolucaoMesaPayload {
	descricao: string;
	data: string;
}

export interface AuditoriaMesaPayload {
	resultado: MesaAuditoriaResultado;
	acao?: string;
}

export async function obterMesas(
	params: MesaFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Mesa5S>> {
	const qs = montarQuery(params);
	return await apiFetch<Paginado<Mesa5S>>(`${BASE}${qs}`, { headers: bearer() }, fetchFn);
}

export function criarMesa(payload: CriarMesaPayload): Promise<Mesa5S> {
	return apiFetch<Mesa5S>(BASE, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export async function obterMesaPorId(id: string, fetchFn: typeof fetch = fetch): Promise<Mesa5S> {
	return await apiFetch<Mesa5S>(`${BASE}/${id}`, { headers: bearer() }, fetchFn);
}

export function registrarEvolucaoMesa(id: string, payload: EvolucaoMesaPayload): Promise<Mesa5S> {
	return apiFetch<Mesa5S>(`${BASE}/${id}/evolucao`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function registrarAuditoriaMesa(id: string, payload: AuditoriaMesaPayload): Promise<Mesa5S> {
	return apiFetch<Mesa5S>(`${BASE}/${id}/auditoria`, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}