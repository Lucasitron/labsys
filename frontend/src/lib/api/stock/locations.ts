import type { CreateLocationPayload, LocalizacaoResp } from '$lib/types/stock';
import { stockFetch } from './request';

export async function listarLocalizacoes(
	fetchFn: typeof fetch = fetch
): Promise<LocalizacaoResp[]> {
	return stockFetch<LocalizacaoResp[]>('/estoque/localizacoes', {}, fetchFn);
}

export async function criarLocalizacao(payload: CreateLocationPayload): Promise<LocalizacaoResp> {
	return stockFetch<LocalizacaoResp>('/estoque/localizacoes', {
		method: 'POST',
		body: JSON.stringify(payload)
	});
}