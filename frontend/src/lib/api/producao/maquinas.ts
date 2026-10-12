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
	'Em manutenção': 'MANUTENCAO'
};

export interface AtualizarStatusMaquinaPayload {
	status: MaquinaStatus;
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
		// Legado 'SUSPENSA' sem par no back: lê como 'Em manutenção', nunca envia.
		status: (raw.status === 'SUSPENSA'
			? 'Em manutenção'
			: (MAQUINA_STATUS_UPPER[raw.status] ?? 'Disponível')) as MaquinaStatus,
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
