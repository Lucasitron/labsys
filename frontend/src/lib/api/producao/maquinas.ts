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
	descricao?: string;
	localizacao?: string;
	status?: MaquinaStatus;
	// Legado sem par no backend (mantido p/ compat de chamada; não enviado).
	categoria?: string;
	responsavelManutencaoId?: string;
}

const MAQUINA_STATUS_PARA_UPPER: Record<string, string> = {
	Disponível: 'DISPONIVEL',
	'Em uso': 'EM_USO',
	'Em manutenção': 'MANUTENCAO',
	Suspensa: 'SUSPENSA'
};

export interface AtualizarStatusMaquinaPayload {
	status: MaquinaStatus;
}

export interface RegistrarUsoMaquinaPayload {
	idFuncionario: string;
	dataInicio?: string;
	observacao?: string;
	// Legado sem par no backend (mantido p/ compat de chamada; não enviado).
	receita?: string;
	membroId?: string;
	inicioEm?: string;
}

export interface EncerrarUsoMaquinaPayload {
	idUso: string;
	dataFim?: string;
	observacao?: string;
}

export interface RawMaquina {
	id: number | string;
	nome: string;
	descricao?: string | null;
	localizacao?: string | null;
	status: string;
}

export function mapMaquina(raw: RawMaquina): Maquina {
	return {
		id: String(raw.id),
		codigo: '',
		nome: raw.nome,
		categoria: raw.localizacao ?? raw.descricao ?? '',
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
	const body: Record<string, unknown> = { nome: payload.nome };
	const localizacao = payload.localizacao ?? payload.categoria;
	if (payload.descricao) body['descricao'] = payload.descricao;
	if (localizacao) body['localizacao'] = localizacao;
	if (payload.status !== undefined) body['status'] = MAQUINA_STATUS_PARA_UPPER[payload.status];
	return apiFetch<Maquina>(BASE, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(body)
	});
}

export function atualizarStatusMaquina(id: string, payload: AtualizarStatusMaquinaPayload): Promise<Maquina> {
	const status = MAQUINA_STATUS_PARA_UPPER[payload.status];
	if (!status) {
		throw new Error(`Status de máquina sem par no backend: ${payload.status}.`);
	}
	return apiFetch<Maquina>(`${BASE}/${id}/status`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify({ status })
	});
}

export function registrarUsoMaquina(id: string, payload: RegistrarUsoMaquinaPayload): Promise<Maquina> {
	const idFuncionario = Number(payload.idFuncionario ?? payload.membroId);
	if (!Number.isInteger(idFuncionario)) {
		throw new Error('Informe o funcionário para iniciar o uso da máquina.');
	}
	const body: Record<string, unknown> = { idFuncionario };
	const dataInicio = payload.dataInicio ?? payload.inicioEm;
	if (dataInicio) body['dataInicio'] = dataInicio;
	if (payload.observacao) body['observacao'] = payload.observacao;
	return apiFetch<Maquina>(`${BASE}/${id}/uso`, {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(body)
	});
}

export function encerrarUsoMaquina(id: string, payload: EncerrarUsoMaquinaPayload): Promise<Maquina> {
	const body: Record<string, unknown> = {};
	if (payload.dataFim) body['dataFim'] = payload.dataFim;
	if (payload.observacao) body['observacao'] = payload.observacao;
	return apiFetch<Maquina>(`${BASE}/${id}/uso/${encodeURIComponent(payload.idUso)}/fim`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(body)
	});
}

export async function obterHistoricoMaquina(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<Maquina[]> {
	const res = await apiFetch<Maquina[] | { data: Maquina[] }>(`${BASE}/${id}/historico`, { headers: bearer() }, fetchFn);
	return unwrapLista(res);
}

// Sem par no backend (F42, Lote 3): gateados com mensagem, sem mapear no vazio.
export function abrirChamadoMaquina(): Promise<never> {
	return Promise.reject(
		new Error('Chamados de máquina indisponíveis — sem par no backend (Lote 3).')
	);
}

export async function obterChamadosDaMaquina(): Promise<ChamadoMaquina[]> {
	throw new Error('Chamados de máquina indisponíveis — sem par no backend (Lote 3).');
}

export async function obterPreventivasDaMaquina(): Promise<Preventiva[]> {
	throw new Error('Preventivas de máquina indisponíveis — sem par no backend (Lote 3).');
}

export function criarPreventivaMaquina(): Promise<never> {
	return Promise.reject(
		new Error('Preventivas de máquina indisponíveis — sem par no backend (Lote 3).')
	);
}

export function registrarHistoricoMaquina(): Promise<never> {
	return Promise.reject(
		new Error('Histórico manual indisponível — use início/fim de uso (GET histórico servido).')
	);
}