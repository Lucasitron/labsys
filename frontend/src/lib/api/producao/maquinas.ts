import type { Cartao5S, ChamadoMaquina, Maquina, MaquinaStatus, Paginado, Preventiva } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, montarQuery } from './client';

const BASE = '/producao/maquinas';

export type MaquinaFiltros = {
	page?: number;
	pageSize?: number;
	search?: string;
	categoria?: string[];
	cartao?: Cartao5S[];
};

export interface CriarMaquinaPayload {
	nome: string;
	categoria: string;
	responsavelManutencaoId: string;
}

export interface AtualizarStatusMaquinaPayload {
	status: MaquinaStatus;
}

export interface RegistrarUsoMaquinaPayload {
	receita: string;
	membroId: string;
	inicioEm: string;
}

export interface AbrirChamadoMaquinaPayload {
	classificacao: string;
	relato: string;
	responsavelId: string;
}

export interface CriarPreventivaMaquinaPayload {
	dataProgramada: string;
}

export interface RegistrarHistoricoMaquinaPayload {
	tipo: 'uso' | 'manutencao';
	descricao: string;
	data: string;
	membroId: string;
}

export async function obterMaquinas(
	params: MaquinaFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Maquina>> {
	const qs = montarQuery(params);
	return await apiFetch<Paginado<Maquina>>(`${BASE}${qs}`, { headers: bearer() }, fetchFn);
}

export async function obterMaquinaPorId(id: string, fetchFn: typeof fetch = fetch): Promise<Maquina> {
	return await apiFetch<Maquina>(`${BASE}/${id}`, { headers: bearer() }, fetchFn);
}

export function criarMaquina(payload: CriarMaquinaPayload): Promise<Maquina> {
	return apiFetch<Maquina>(BASE, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function atualizarStatusMaquina(id: string, payload: AtualizarStatusMaquinaPayload): Promise<Maquina> {
	return apiFetch<Maquina>(`${BASE}/${id}/status`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function registrarUsoMaquina(id: string, payload: RegistrarUsoMaquinaPayload): Promise<Maquina> {
	return apiFetch<Maquina>(`${BASE}/${id}/uso`, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function abrirChamadoMaquina(id: string, payload: AbrirChamadoMaquinaPayload): Promise<ChamadoMaquina> {
	return apiFetch<ChamadoMaquina>(`${BASE}/${id}/chamados`, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export async function obterPreventivasDaMaquina(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<Preventiva[]> {
	return await apiFetch<Preventiva[]>(`${BASE}/${id}/preventivas`, { headers: bearer() }, fetchFn);
}

export function criarPreventivaMaquina(id: string, payload: CriarPreventivaMaquinaPayload): Promise<Preventiva> {
	return apiFetch<Preventiva>(`${BASE}/${id}/preventivas`, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function registrarHistoricoMaquina(id: string, payload: RegistrarHistoricoMaquinaPayload): Promise<Maquina> {
	return apiFetch<Maquina>(`${BASE}/${id}/historico`, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export async function obterChamadosDaMaquina(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<ChamadoMaquina[]> {
	return await apiFetch<ChamadoMaquina[]>(`${BASE}/${id}/chamados`, { headers: bearer() }, fetchFn);
}