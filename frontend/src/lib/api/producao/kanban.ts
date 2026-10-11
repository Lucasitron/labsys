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
}

export interface RawKanban {
	id: number | string;
	idEncomenda: number | string;
	status: string;
	idResponsavel?: number | string | null;
}

export function mapKanban(raw: RawKanban): KanbanItem {
	return {
		id: String(raw.id),
		codigo: '',
		titulo: `#${raw.idEncomenda}`,
		coluna: (KANBAN_UPPER[raw.status] ?? 'Fila') as KanbanColuna,
		responsavel:
			raw.idResponsavel != null ? { id: String(raw.idResponsavel), nome: '' } : null
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
	return apiFetch<KanbanItem>(`${BASE}/${id}/mover`, {
		method: 'PATCH',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}