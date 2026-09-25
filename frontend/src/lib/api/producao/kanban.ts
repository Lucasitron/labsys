import type { KanbanColuna, KanbanItem } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, montarQuery } from './client';

const BASE = '/producao/kanban';

export type KanbanFiltros = {
	tab?: KanbanColuna;
	status?: KanbanColuna[];
};

export interface MoverKanbanItemPayload {
	coluna: KanbanColuna;
}

export async function obterKanban(
	params: KanbanFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<KanbanItem[]> {
	const qs = montarQuery(params);
	return await apiFetch<KanbanItem[]>(`${BASE}${qs}`, { headers: bearer() }, fetchFn);
}

export function moverKanbanItem(id: string, payload: MoverKanbanItemPayload): Promise<KanbanItem> {
	return apiFetch<KanbanItem>(`${BASE}/${id}/mover`, {
		method: 'PATCH',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}