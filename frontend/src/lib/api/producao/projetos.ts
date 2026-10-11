import type { MaterialProjeto, Paginado, Projeto, ProjetoDocumento, ProjetoStatus, SaidaMaterial, TipoDocumentoProjeto } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, paginarClient, STATUS_PROJETO_UPPER, unwrapLista } from './client';

const BASE = '/producao/projetos';

export type ProjetoFiltros = {
	status?: ProjetoStatus[];
	responsavel?: string[];
	page?: number;
	pageSize?: number;
};

export interface CriarProjetoPayload {
	nome: string;
	descricao?: string;
	dataInicio: string;
	dataFimPrevista?: string;
	idResponsavel: string;
	status?: ProjetoStatus;
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

function validarUrlDocumento(url: string): string {
	let resultado: URL;
	try {
		resultado = new URL(url);
	} catch {
		throw new Error('URL de documento inválida. Informe um endereço http:// ou https://.');
	}
	if (resultado.protocol !== 'http:' && resultado.protocol !== 'https:') {
		throw new Error('URL de documento inválida. Apenas http:// e https:// são aceitos.');
	}
	return resultado.toString();
}

const PROJETO_STATUS_PARA_UPPER: Record<ProjetoStatus, string> = {
	Planejado: 'PLANEJADO',
	'Em andamento': 'EM_ANDAMENTO',
	Concluído: 'CONCLUIDO',
	Cancelado: 'CANCELADO'
};

function paraPayloadProjeto(payload: CriarProjetoPayload | Partial<CriarProjetoPayload>): Record<string, unknown> {
	const idResponsavel = Number(
		(payload as CriarProjetoPayload).idResponsavel ?? (payload as { responsavelId?: string }).responsavelId
	);
	if (!Number.isInteger(idResponsavel)) {
		throw new Error('Informe o responsável pelo projeto.');
	}
	const dataInicio =
		(payload as CriarProjetoPayload).dataInicio ?? (payload as { prazo?: string }).prazo;
	if (!dataInicio) {
		throw new Error('Informe a data de início do projeto.');
	}
	const body: Record<string, unknown> = {
		nome: (payload as CriarProjetoPayload).nome,
		dataInicio,
		idResponsavel
	};
	if (payload.descricao !== undefined) body['descricao'] = payload.descricao;
	const dataFim =
		(payload as CriarProjetoPayload).dataFimPrevista ?? (payload as { prazo?: string }).prazo;
	if (dataFim) body['dataFimPrevista'] = dataFim;
	if (payload.status !== undefined) body['status'] = PROJETO_STATUS_PARA_UPPER[payload.status];
	return body;
}

export interface RawProjeto {
	id: number | string;
	nome: string;
	descricao?: string | null;
	dataInicio?: string | null;
	dataFimPrevista?: string | null;
	status: string;
	idResponsavel: number | string;
}

export function mapProjeto(raw: RawProjeto): Projeto {
	return {
		id: String(raw.id),
		codigo: '',
		nome: raw.nome,
		descricao: raw.descricao ?? undefined,
		responsavel: { id: String(raw.idResponsavel), nome: '' },
		prazo: raw.dataFimPrevista ?? '',
		...(raw.dataInicio ? { dataInicio: raw.dataInicio.slice(0, 10) } : {}),
		status: (STATUS_PROJETO_UPPER[raw.status] ?? 'Planejado') as ProjetoStatus,
		progresso: 0
	};
}

export async function obterProjetos(
	params: ProjetoFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Projeto>> {
	const res = await apiFetch<RawProjeto[] | { data: RawProjeto[] }>(BASE, { headers: bearer() }, fetchFn);
	let todos = unwrapLista(res).map(mapProjeto);
	if (params.status && params.status.length > 0) {
		todos = todos.filter((p) => params.status!.includes(p.status));
	}
	if (params.responsavel && params.responsavel.length > 0) {
		todos = todos.filter((p) => params.responsavel!.includes(p.responsavel.id));
	}
	return paginarClient(todos, params.page, params.pageSize);
}

export async function obterProjetoPorId(id: string, fetchFn: typeof fetch = fetch): Promise<Projeto> {
	return await apiFetch<Projeto>(`${BASE}/${id}`, { headers: bearer() }, fetchFn);
}

export function criarProjeto(payload: CriarProjetoPayload): Promise<Projeto> {
	return apiFetch<Projeto>(BASE, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(paraPayloadProjeto(payload))
	});
}

export function atualizarProjeto(id: string, payload: Partial<CriarProjetoPayload>): Promise<Projeto> {
	return apiFetch<Projeto>(`${BASE}/${id}`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(paraPayloadProjeto(payload))
	});
}

export async function obterDocumentosDoProjeto(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<ProjetoDocumento[]> {
	return await apiFetch<ProjetoDocumento[]>(`${BASE}/${id}/documentos`, { headers: bearer() }, fetchFn);
}

export function criarDocumentoDoProjeto(id: string, payload: CriarDocumentoPayload): Promise<ProjetoDocumento> {
	const url = validarUrlDocumento(payload.url);
	return apiFetch<ProjetoDocumento>(`${BASE}/${id}/documentos`, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify({ ...payload, url })
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