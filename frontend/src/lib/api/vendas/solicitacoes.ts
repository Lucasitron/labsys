import { get } from 'svelte/store';
import { ApiError, apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	CreateSolicitacaoPayload,
	DecidirSolicitacaoPayload,
	ListSolicitacoesParams,
	SolicitacaoEdicao,
	SolicitacoesResult
} from '$lib/types/vendas';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function listSolicitacoes(
	params: ListSolicitacoesParams = {},
	fetchFn: typeof fetch = fetch
): Promise<SolicitacoesResult> {
	const query = new URLSearchParams();
	if (params.status) query.set('status', params.status);
	const qs = query.toString();
	return await apiFetch<SolicitacoesResult>(
		`/api/vendas/solicitacoes${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function createSolicitacao(
	payload: CreateSolicitacaoPayload,
	fetchFn: typeof fetch = fetch
): Promise<SolicitacaoEdicao> {
	return await apiFetch<SolicitacaoEdicao>(
		'/api/vendas/solicitacoes',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function decidirSolicitacao(
	id: string,
	decisao: DecidirSolicitacaoPayload,
	fetchFn: typeof fetch = fetch
): Promise<SolicitacaoEdicao> {
	if (!decisao.aprovada && !decisao.motivo?.trim()) {
		throw new ApiError(400, 'MOTIVO_OBRIGATORIO', 'Informe o motivo da rejeição.');
	}
	return await apiFetch<SolicitacaoEdicao>(
		`/api/vendas/solicitacoes/${id}`,
		{ method: 'PUT', headers: bearer(), body: JSON.stringify(decisao) },
		fetchFn
	);
}
