import type { CreateLocationPayload, LocalizacaoResp } from '$lib/types/stock';
import { apiFetch } from '$lib/api/client';

export async function listarLocalizacoes(
	fetchFn: typeof fetch = fetch
): Promise<LocalizacaoResp[]> {
	return apiFetch<LocalizacaoResp[]>('/estoque/localizacoes', {}, fetchFn);
}

export async function criarLocalizacao(payload: CreateLocationPayload): Promise<LocalizacaoResp> {
	return apiFetch<LocalizacaoResp>('/estoque/localizacoes', {
		method: 'POST',
		body: JSON.stringify(payload)
	});
}