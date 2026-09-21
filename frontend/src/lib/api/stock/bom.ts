import type {
	BomResponse,
	CreateBomPayload,
	ProjectOption,
	RegistrarConsumoPayload
} from '$lib/types/stock';
import { stockFetch } from './request';

export async function buscarBom(id: string, fetchFn: typeof fetch = fetch): Promise<BomResponse> {
	return stockFetch<BomResponse>(`/estoque/boms/${id}`, {}, fetchFn);
}

export async function criarBom(payload: CreateBomPayload): Promise<BomResponse> {
	return stockFetch<BomResponse>('/estoque/boms', {
		method: 'POST',
		body: JSON.stringify(payload)
	});
}

export async function atualizarBom(
	id: string,
	payload: CreateBomPayload
): Promise<BomResponse> {
	return stockFetch<BomResponse>(`/estoque/boms/${id}`, {
		method: 'PUT',
		body: JSON.stringify(payload)
	});
}

export async function registrarConsumo(payload: RegistrarConsumoPayload): Promise<void> {
	await stockFetch<unknown>('/estoque/boms/consumo', {
		method: 'POST',
		body: JSON.stringify(payload)
	});
}

// 🔴 Est-008 (TODO): backend /estoque/boms/{id}/pedido — sem endpoint definido ainda.
export function generateOrder(_bomId: string): Promise<never> {
	return Promise.reject(new Error('Pedido a partir de BOM não disponível (TODO)'));
}

// 🔴 Est-008 (TODO): backend não expõe listagem de projetos — tela de saída usa lista vazia.
export async function fetchProjects(_fetchFn: typeof fetch = fetch, _search = ''): Promise<ProjectOption[]> {
	return [];
}