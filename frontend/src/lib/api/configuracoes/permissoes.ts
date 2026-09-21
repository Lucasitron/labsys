import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type { Module } from '$lib/types/auth';
import type { Nivel, PermissaoMatriz, PermissaoNivel } from '$lib/types/configuracoes';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

// TODO contrato 🟡: endpoint assumido — GET /permissoes (matriz completa)
// Fonte única consumida por utils/permissions.ts; nada é persistido localmente.
export async function obterPermissoes(fetchFn: typeof fetch = fetch): Promise<PermissaoMatriz> {
	return apiFetch<PermissaoMatriz>('/permissoes', { headers: bearer() }, fetchFn);
}

// TODO contrato 🟡: endpoint assumido — PUT /permissoes/{modulo}/{nivel}
export function atualizarPermissao(
	modulo: Module,
	nivel: Nivel,
	valor: PermissaoNivel
): Promise<PermissaoMatriz> {
	return apiFetch<PermissaoMatriz>(`/permissoes/${modulo}/${nivel}`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify({ valor })
	});
}