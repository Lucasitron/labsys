import type {
	CreateEntradaPayload,
	CreateSaidaPayload,
	EntradaResponse,
	SaidaResponse
} from '$lib/types/stock';
import { buildQuery, stockFetch } from './request';

// ---- Entradas ----

function mapEntrada(raw: EntradaResponse): EntradaResponse {
	return {
		...raw,
		quantidade: Number(raw.quantidade),
		valorUnitario: raw.valorUnitario == null ? raw.valorUnitario : Number(raw.valorUnitario)
	};
}

function mapSaida(raw: SaidaResponse): SaidaResponse {
	return { ...raw, quantidade: Number(raw.quantidade) };
}

export async function listarEntradasPorItem(
	idItem: string,
	fetchFn: typeof fetch = fetch
): Promise<EntradaResponse[]> {
	const raw = await stockFetch<EntradaResponse[]>(`/estoque/entradas${buildQuery({ idItem })}`, {}, fetchFn);
	return raw.map(mapEntrada);
}

export async function buscarEntrada(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<EntradaResponse> {
	return mapEntrada(await stockFetch<EntradaResponse>(`/estoque/entradas/${id}`, {}, fetchFn));
}

export async function criarEntrada(payload: CreateEntradaPayload): Promise<EntradaResponse> {
	return mapEntrada(await stockFetch<EntradaResponse>('/estoque/entradas', {
		method: 'POST',
		body: JSON.stringify(payload)
	}));
}

// ---- Saídas ----

export async function listarSaidasPorItem(
	idItem: string,
	fetchFn: typeof fetch = fetch
): Promise<SaidaResponse[]> {
	const raw = await stockFetch<SaidaResponse[]>(`/estoque/saidas${buildQuery({ idItem })}`, {}, fetchFn);
	return raw.map(mapSaida);
}

export async function buscarSaida(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<SaidaResponse> {
	return mapSaida(await stockFetch<SaidaResponse>(`/estoque/saidas/${id}`, {}, fetchFn));
}

export async function criarSaida(payload: CreateSaidaPayload): Promise<SaidaResponse> {
	return mapSaida(await stockFetch<SaidaResponse>('/estoque/saidas', {
		method: 'POST',
		body: JSON.stringify(payload)
	}));
}