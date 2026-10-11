import type { KanbanColuna, KanbanItem } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, KANBAN_UPPER, unwrapLista } from './client';

const BASE = '/producao/kanban';

export type KanbanFiltros = {
	tab?: KanbanColuna;
	status?: KanbanColuna[];
};

export interface MoverKanbanItemPayload {
	coluna: KanbanColuna;
	version: number;
	observacao?: string;
}

const KANBAN_PARA_UPPER: Record<KanbanColuna, string> = {
	Fila: 'FILA',
	Produção: 'PRODUCAO',
	Acabamento: 'ACABAMENTO',
	Pronto: 'PRONTO',
	Entregue: 'ENTREGUE'
};

export interface RawKanban {
	id: number | string;
	idEncomenda: number | string;
	status: string;
	idResponsavel?: number | string | null;
	version?: number | string | null;
}

export function mapKanban(raw: RawKanban): KanbanItem & { version: number } {
	return {
		id: String(raw.id),
		codigo: '',
		titulo: `#${raw.idEncomenda}`,
		coluna: (KANBAN_UPPER[raw.status] ?? 'Fila') as KanbanColuna,
		responsavel:
			raw.idResponsavel != null ? { id: String(raw.idResponsavel), nome: '' } : null,
		version: raw.version != null ? Number(raw.version) : 0
	};
}

export async function obterKanban(
	params: KanbanFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<KanbanItem[]> {
	const res = await apiFetch<RawKanban[] | { data: RawKanban[] }>(BASE, { headers: bearer() }, fetchFn);
	let itens = unwrapLista(res).map(mapKanban);
	const colunas = params.status ?? (params.tab ? [params.tab] : undefined);
	if (colunas && colunas.length > 0) {
		itens = itens.filter((i) => colunas.includes(i.coluna));
	}
	return itens;
}

export function moverKanbanItem(id: string, payload: MoverKanbanItemPayload): Promise<KanbanItem> {
	if (!Number.isInteger(payload.version)) {
		throw new Error('Versão do cartão exigida para mover no kanban.');
	}
	const body: Record<string, unknown> = {
		statusNovo: KANBAN_PARA_UPPER[payload.coluna],
		version: payload.version
	};
	if (payload.observacao) body['observacao'] = payload.observacao;
	return apiFetch<KanbanItem>(`${BASE}/${id}/mover`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(body)
	});
}