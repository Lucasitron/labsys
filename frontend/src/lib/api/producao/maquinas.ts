import type { Cartao5S, ChamadoMaquina, Maquina, MaquinaStatus, Paginado, Preventiva } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, MAQUINA_STATUS_UPPER, paginarClient, unwrapLista } from './client';

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

export interface RawMaquina {
	id: number | string;
	nome: string;
	localizacao?: string | null;
	status: string;
}

export function mapMaquina(raw: RawMaquina): Maquina {
	return {
		id: String(raw.id),
		codigo: '',
		nome: raw.nome,
		categoria: raw.localizacao ?? '',
		responsavelManutencao: { id: '', nome: '' },
		status: (MAQUINA_STATUS_UPPER[raw.status] ?? 'Disponível') as MaquinaStatus,
		cartao: 'Verde' as Cartao5S
	};
}

export async function obterMaquinas(
	params: MaquinaFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Maquina>> {
	const res = await apiFetch<RawMaquina[] | { data: RawMaquina[] }>(BASE, { headers: bearer() }, fetchFn);
	let todas = unwrapLista(res).map(mapMaquina);
	if (params.search) {
		const termo = params.search.toLowerCase();
		todas = todas.filter(
			(m) => m.nome.toLowerCase().includes(termo) || m.categoria.toLowerCase().includes(termo)
		);
	}
	if (params.categoria && params.categoria.length > 0) {
		todas = todas.filter((m) => params.categoria!.includes(m.categoria));
	}
	return paginarClient(todas, params.page, params.pageSize);
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