import type { Inspecao5S, InspecaoStatus, Paginado } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, paraPaginado, unwrapLista } from './client';

const BASE = '/producao/inspecoes-5s';

export type InspecaoFiltros = {
	status?: InspecaoStatus[];
	setor?: string[];
	auditor?: string[];
	page?: number;
	pageSize?: number;
};

export interface RawInspecao {
	id: number | string;
	idSetor: number | string;
	setorNome?: string | null;
	idInspetor: number | string;
	dataInspecao: string;
	turno?: string | null;
	status?: string | null;
	observacoes?: string | null;
	itens?: { idChecklist?: number | string | null; conforme?: boolean | null }[] | null;
}

const INSPECAO_STATUS_UPPER: Record<string, InspecaoStatus> = {
	OK: 'Concluída',
	NAO_CONFORME: 'Concluída'
};

// Adaptador de leitura: status do back é recalculado (OK/NAO_CONFORME);
// nota/cartão não têm par (neutros, sem inventar).
export function mapInspecao(raw: RawInspecao): Inspecao5S {
	return {
		id: String(raw.id),
		setor: { id: String(raw.idSetor), nome: raw.setorNome ?? '' },
		auditor: { id: String(raw.idInspetor), nome: '' },
		data: (raw.dataInspecao ?? '').slice(0, 10),
		status: (raw.status ? (INSPECAO_STATUS_UPPER[raw.status] ?? 'Programada') : 'Programada'),
		nota: null,
		cartao: null,
		itens: (raw.itens ?? []).map((i, idx) => ({
			s: i.idChecklist != null ? Number(i.idChecklist) : idx,
			conforme: i.conforme ?? false
		}))
	};
}

export async function obterInspecoes5S(
	params: InspecaoFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Inspecao5S>> {
	const res = await apiFetch<RawInspecao[] | { data: RawInspecao[] }>(BASE, { headers: bearer() }, fetchFn);
	return paraPaginado({ data: unwrapLista(res).map(mapInspecao) }, params.page, params.pageSize);
}

// Sem par no backend (F44, só POST/GET/GET{id}/DELETE): gateadas com mensagem.
export function concluirInspecao5S(): Promise<never> {
	return Promise.reject(
		new Error('Concluir inspeção indisponível — sem par no backend (Lote 3).')
	);
}

export function nomearAuditor5S(): Promise<never> {
	return Promise.reject(
		new Error('Nomear auditor indisponível — sem par no backend (Lote 3).')
	);
}