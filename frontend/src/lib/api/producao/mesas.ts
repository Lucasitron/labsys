import type { Mesa5S, MesaAuditoriaResultado, Paginado, SituacaoMesas } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, paginarClient, unwrapLista } from './client';

const BASE = '/producao/projetos-mesa';
const AUDITORIAS = '/producao/auditorias-projeto-mesa';

export type MesaFiltros = {
	mes?: string;
	situacao?: SituacaoMesas[];
	search?: string;
	page?: number;
	pageSize?: number;
};

export interface CriarMesaPayload {
	idFuncionario: string;
	idMesa: string;
	nomeProjeto: string;
	tipoProjeto?: string;
	prazoExecucao?: string;
}

export interface AuditoriaMesaPayload {
	resultado: MesaAuditoriaResultado | 'CONCLUIDO';
	acao?: string;
}

export interface RawProjetoMesa {
	id: number | string;
	idFuncionario: number | string;
	idMesa: number | string;
	nomeProjeto: string;
	tipoProjeto?: string | null;
	prazoExecucao?: string | null;
	dataInicio?: string | null;
	dataUltimaEvolucao?: string | null;
	status: string;
	qrCodeTotem?: string | null;
}

// Adaptador de leitura: status ATIVO→Aprovada, demais→Pendente (sem par 1:1
// no front); período experimental sem par (neutro, sem inventar).
export function mapMesa(raw: RawProjetoMesa): Mesa5S {
	return {
		id: String(raw.id),
		nome: raw.nomeProjeto,
		projeto: { id: String(raw.idMesa), codigo: '' },
		membro: { id: String(raw.idFuncionario), nome: '' },
		status: (raw.status === 'ATIVO' ? 'Aprovada' : 'Pendente') as SituacaoMesas,
		qrTotem: raw.qrCodeTotem ?? '',
		periodoExperimental: false,
		ultimaAuditoria: null
	};
}

export async function obterMesas(
	params: MesaFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Mesa5S>> {
	const res = await apiFetch<RawProjetoMesa[] | { data: RawProjetoMesa[] }>(
		BASE,
		{ headers: bearer() },
		fetchFn
	);
	let todas = unwrapLista(res).map(mapMesa);
	if (params.search) {
		const termo = params.search.toLowerCase();
		todas = todas.filter((m) => m.nome.toLowerCase().includes(termo));
	}
	if (params.situacao && params.situacao.length > 0) {
		todas = todas.filter((m) => params.situacao!.includes(m.status));
	}
	return paginarClient(todas, params.page, params.pageSize);
}

export function criarMesa(payload: CriarMesaPayload): Promise<Mesa5S> {
	const idFuncionario = Number(payload.idFuncionario);
	const idMesa = Number(payload.idMesa);
	if (!Number.isInteger(idFuncionario)) throw new Error('Informe o membro responsável.');
	if (!Number.isInteger(idMesa)) throw new Error('Informe o número da mesa.');
	if (!payload.nomeProjeto?.trim()) throw new Error('Informe o nome do projeto.');
	const body: Record<string, unknown> = {
		idFuncionario,
		idMesa,
		nomeProjeto: payload.nomeProjeto.trim()
	};
	if (payload.tipoProjeto) body['tipoProjeto'] = payload.tipoProjeto;
	if (payload.prazoExecucao) body['prazoExecucao'] = payload.prazoExecucao;
	return apiFetch<Mesa5S>(BASE, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(body)
	});
}

export async function obterMesaPorId(id: string, fetchFn: typeof fetch = fetch): Promise<Mesa5S> {
	const raw = await apiFetch<RawProjetoMesa>(`${BASE}/${id}`, { headers: bearer() }, fetchFn);
	return mapMesa(raw);
}

export function registrarEvolucaoMesa(id: string): Promise<Mesa5S> {
	return apiFetch<Mesa5S>(`${BASE}/${id}/evolucao`, {
		method: 'PUT',
		headers: bearer()
	});
}

export function registrarAuditoriaMesa(id: string, payload: AuditoriaMesaPayload): Promise<void> {
	const body: Record<string, unknown> = {
		idProjetoMesa: Number(id),
		resultado: payload.resultado
	};
	if (payload.acao) body['acaoTomada'] = payload.acao;
	return apiFetch<void>(AUDITORIAS, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(body)
	});
}

export async function obterAuditoriasMesa(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<{ resultado: string; data: string; acao?: string }[]> {
	return await apiFetch<{ resultado: string; data: string; acao?: string }[]>(
		`${AUDITORIAS}/${encodeURIComponent(id)}`,
		{ headers: bearer() },
		fetchFn
	);
}
