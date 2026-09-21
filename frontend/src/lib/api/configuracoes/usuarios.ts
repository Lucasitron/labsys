import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type { Nivel, Paginado, SituacaoUsuario, Usuario } from '$lib/types/configuracoes';

export interface UsuarioPayload {
	nome: string;
	email: string;
	telefone?: string;
	nivel: Nivel;
	situacao?: SituacaoUsuario;
}

export interface UsuariosFiltros {
	page?: number;
	search?: string;
	nivel?: Nivel;
	situacao?: SituacaoUsuario;
}

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

function montarQuery(filtros: UsuariosFiltros): string {
	const search = new URLSearchParams();
	if (filtros.page !== undefined) search.set('page', String(filtros.page));
	if (filtros.search) search.set('search', filtros.search);
	if (filtros.nivel !== undefined) search.set('nivel', String(filtros.nivel));
	if (filtros.situacao) search.set('situacao', filtros.situacao);
	const qs = search.toString();
	return qs ? `?${qs}` : '';
}

// TODO contrato 🟡: endpoint assumido — GET /usuarios (paginado)
// Ponto de contrato com RH: Configurações opera contas/nível/situação; o cadastro físico é do domínio /rh.
export async function listarUsuarios(
	filtros: UsuariosFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Usuario>> {
	return apiFetch<Paginado<Usuario>>(
		`/usuarios${montarQuery(filtros)}`,
		{ headers: bearer() },
		fetchFn
	);
}

// TODO contrato 🟡: endpoint assumido — POST /usuarios (convite)
export function criarUsuario(payload: UsuarioPayload): Promise<Usuario> {
	return apiFetch<Usuario>('/usuarios', {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

// TODO contrato 🟡: endpoint assumido — PUT /usuarios/{id}
export function atualizarUsuario(id: string, payload: UsuarioPayload): Promise<Usuario> {
	return apiFetch<Usuario>(`/usuarios/${id}`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

// TODO contrato 🟡: endpoint assumido — PATCH /usuarios/{id}/status
export function alterarStatus(id: string, situacao: SituacaoUsuario): Promise<Usuario> {
	return apiFetch<Usuario>(`/usuarios/${id}/status`, {
		method: 'PATCH',
		headers: bearer(),
		body: JSON.stringify({ situacao })
	});
}