import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type { ParametroSistema, TokenIntegracao } from '$lib/types/configuracoes';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

// TODO contrato 🟡: endpoint assumido — GET /configuracoes/sistema
export async function obterSistema(fetchFn: typeof fetch = fetch): Promise<ParametroSistema> {
	return apiFetch<ParametroSistema>('/configuracoes/sistema', { headers: bearer() }, fetchFn);
}

// TODO contrato 🟡: endpoint assumido — PUT /configuracoes/sistema
// Reconciliação: cadenciaChecklist5S/cadenciaAuditoria5S são compartilhadas com
// producao/parametros-5s (cadência 5S) — mesmo dado, contratos distintos.
export function atualizarSistema(payload: ParametroSistema): Promise<ParametroSistema> {
	return apiFetch<ParametroSistema>('/configuracoes/sistema', {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

// TODO contrato 🟡: endpoint assumido — GET /configuracoes/tokens
export async function listarTokens(fetchFn: typeof fetch = fetch): Promise<TokenIntegracao[]> {
	return apiFetch<TokenIntegracao[]>('/configuracoes/tokens', { headers: bearer() }, fetchFn);
}

// TODO contrato 🟡: endpoint assumido — POST /configuracoes/tokens
export function criarToken(payload: { nome: string }): Promise<TokenIntegracao> {
	return apiFetch<TokenIntegracao>('/configuracoes/tokens', {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

// TODO contrato 🟡: endpoint assumido — DELETE /configuracoes/tokens/{id}
export function revogarToken(id: string): Promise<void> {
	return apiFetch<void>(`/configuracoes/tokens/${id}`, {
		method: 'DELETE',
		headers: bearer()
	});
}