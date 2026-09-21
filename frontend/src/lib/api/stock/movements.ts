import type {
	CreateEntradaPayload,
	CreateSaidaPayload,
	EntradaResponse,
	SaidaResponse
} from '$lib/types/stock';
import { buildQuery, stockFetch } from './request';

// ---- Entradas ----

export async function listarEntradasPorItem(
	idItem: string,
	fetchFn: typeof fetch = fetch
): Promise<EntradaResponse[]> {
	return stockFetch<EntradaResponse[]>(`/estoque/entradas${buildQuery({ idItem })}`, {}, fetchFn);
}

export async function buscarEntrada(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<EntradaResponse> {
	return stockFetch<EntradaResponse>(`/estoque/entradas/${id}`, {}, fetchFn);
}

export async function criarEntrada(payload: CreateEntradaPayload): Promise<EntradaResponse> {
	return stockFetch<EntradaResponse>('/estoque/entradas', {
		method: 'POST',
		body: JSON.stringify(payload)
	});
}

// ---- Saídas ----

export async function listarSaidasPorItem(
	idItem: string,
	fetchFn: typeof fetch = fetch
): Promise<SaidaResponse[]> {
	return stockFetch<SaidaResponse[]>(`/estoque/saidas${buildQuery({ idItem })}`, {}, fetchFn);
}

export async function buscarSaida(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<SaidaResponse> {
	return stockFetch<SaidaResponse>(`/estoque/saidas/${id}`, {}, fetchFn);
}

export async function criarSaida(payload: CreateSaidaPayload): Promise<SaidaResponse> {
	return stockFetch<SaidaResponse>('/estoque/saidas', {
		method: 'POST',
		body: JSON.stringify(payload)
	});
}