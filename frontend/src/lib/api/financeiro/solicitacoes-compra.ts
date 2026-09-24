import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	SolicitacaoCompra,
	CreateSolicitacaoCompraPayload,
	ConcluirCompraPayload,
	ConcluirCompraResult
} from '$lib/types/financeiro';

const BASE = '/api/financeiro/solicitacoes-compra';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function listSolicitacoesCompra(
	fetchFn: typeof fetch = fetch
): Promise<SolicitacaoCompra[]> {
	return await apiFetch<SolicitacaoCompra[]>(BASE, { headers: bearer() }, fetchFn);
}

export async function createSolicitacaoCompra(
	payload: CreateSolicitacaoCompraPayload,
	fetchFn: typeof fetch = fetch
): Promise<SolicitacaoCompra> {
	return await apiFetch<SolicitacaoCompra>(
		BASE,
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}

export async function concluirCompra(
	id: string,
	payload: ConcluirCompraPayload,
	fetchFn: typeof fetch = fetch
): Promise<ConcluirCompraResult> {
	return await apiFetch<ConcluirCompraResult>(
		`${BASE}/${encodeURIComponent(id)}/concluir`,
		{ method: 'PUT', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}
