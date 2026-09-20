import type { MaterialProjeto, Paginado, Projeto, ProjetoDocumento, ProjetoStatus, SaidaMaterial, TipoDocumentoProjeto } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, montarQuery } from './client';

const BASE = '/producao/projetos';

export type ProjetoFiltros = {
	status?: ProjetoStatus[];
	responsavel?: string[];
	page?: number;
	pageSize?: number;
};

export interface CriarProjetoPayload {
	nome: string;
	cliente?: string;
	descricao?: string;
	responsavelId: string;
	prazo: string;
	status: ProjetoStatus;
}

export interface CriarDocumentoPayload {
	tipo: TipoDocumentoProjeto;
	nome?: string;
	url: string;
}

export interface RegistrarSaidaMaterialPayload {
	item: string;
	codigo: string;
	quantidade: number;
}

export async function obterProjetos(
	params: ProjetoFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Projeto>> {
	const qs = montarQuery(params);
	return await apiFetch<Paginado<Projeto>>(`${BASE}${qs}`, { headers: bearer() }, fetchFn);
}

export async function obterProjetoPorId(id: string, fetchFn: typeof fetch = fetch): Promise<Projeto> {
	return await apiFetch<Projeto>(`${BASE}/${id}`, { headers: bearer() }, fetchFn);
}

export function criarProjeto(payload: CriarProjetoPayload): Promise<Projeto> {
	return apiFetch<Projeto>(BASE, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export function atualizarProjeto(id: string, payload: Partial<CriarProjetoPayload>): Promise<Projeto> {
	return apiFetch<Projeto>(`${BASE}/${id}`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export async function obterDocumentosDoProjeto(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<ProjetoDocumento[]> {
	return await apiFetch<ProjetoDocumento[]>(`${BASE}/${id}/documentos`, { headers: bearer() }, fetchFn);
}

export function criarDocumentoDoProjeto(id: string, payload: CriarDocumentoPayload): Promise<ProjetoDocumento> {
	return apiFetch<ProjetoDocumento>(`${BASE}/${id}/documentos`, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

export async function obterMateriaisDoProjeto(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<MaterialProjeto[]> {
	return await apiFetch<MaterialProjeto[]>(`${BASE}/${id}/materiais`, { headers: bearer() }, fetchFn);
}

export function registrarSaidaMaterial(
	id: string,
	payload: RegistrarSaidaMaterialPayload
): Promise<SaidaMaterial> {
	return apiFetch<SaidaMaterial>(`${BASE}/${id}/materiais/saida`, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}