import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type { Interacao, RegistrarInteracaoPayload } from '$lib/types/vendas';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function listInteracoes(
	clienteId: string,
	fetchFn: typeof fetch = fetch
): Promise<Interacao[]> {
	return await apiFetch<Interacao[]>(
		`/api/vendas/clientes/${clienteId}/interacoes`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function registrarInteracao(
	payload: RegistrarInteracaoPayload,
	fetchFn: typeof fetch = fetch
): Promise<Interacao> {
	return await apiFetch<Interacao>(
		'/api/vendas/interacoes',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}
