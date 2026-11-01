import { apiFetch, buildQuery } from '$lib/api/client';
import type { Nivel, Paginado, SituacaoUsuario, Usuario } from '$lib/types/configuracoes';

export interface UsuarioPayload {
	nome: string;
	email: string;
	nivel: Nivel;
	situacao?: SituacaoUsuario;
}

export interface UsuariosFiltros {
	page?: number;
	pageSize?: number;
	search?: string;
	nivel?: Nivel;
	situacao?: SituacaoUsuario;
}


const SITUACAO_PARA_UPPER: Record<SituacaoUsuario, string> = {
	Ativo: 'ATIVO',
	Pendente: 'PENDENTE',
	Desativado: 'DESATIVADO'
};

const UPPER_PARA_SITUACAO: Record<string, SituacaoUsuario> = {
	ATIVO: 'Ativo',
	PENDENTE: 'Pendente',
	DESATIVADO: 'Desativado'
};

interface RawUsuario {
	id: number | string;
	idUser?: number | string | null;
	email: string;
	nomeUsuario: string;
	setor?: string | null;
	situacao: string;
	nivel?: number | null;
}

export function mapUsuario(raw: RawUsuario): Usuario {
	return {
		id: String(raw.id),
		nome: raw.nomeUsuario,
		email: raw.email,
		telefone: null,
		nivel: (raw.nivel ?? 4) as Nivel,
		situacao: UPPER_PARA_SITUACAO[raw.situacao] ?? 'Pendente',
		foto: null,
		criadoEm: ''
	};
}

export async function listarUsuarios(
	filtros: UsuariosFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Usuario>> {
	const res = await apiFetch<{
		content: RawUsuario[];
		page: number;
		size: number;
		total: number;
		totalPages: number;
	}>(
		`/usuarios${buildQuery({ page: filtros.page !== undefined ? Math.max(0, filtros.page - 1) : undefined, size: filtros.pageSize, search: filtros.search, nivel: filtros.nivel, situacao: filtros.situacao ? SITUACAO_PARA_UPPER[filtros.situacao] : undefined })}`,
		{},
		fetchFn
	);
	return {
		dados: res.content.map(mapUsuario),
		paginacao: {
			page: res.page + 1,
			pageSize: res.size,
			totalItems: res.total,
			totalPages: res.totalPages
		}
	};
}

export function atualizarUsuario(id: string, payload: UsuarioPayload): Promise<Usuario> {
	return apiFetch<RawUsuario>(`/usuarios/${id}`, {
		method: 'PUT',
		body: JSON.stringify({
			email: payload.email,
			nomeUsuario: payload.nome,
			nivel: payload.nivel,
			...(payload.situacao ? { situacao: SITUACAO_PARA_UPPER[payload.situacao] } : {})
		})
	}).then(mapUsuario);
}

export function alterarStatus(id: string, situacao: SituacaoUsuario): Promise<Usuario> {
	return apiFetch<RawUsuario>(`/usuarios/${id}/status`, {
		method: 'PATCH',
		body: JSON.stringify({ situacao: SITUACAO_PARA_UPPER[situacao] })
	}).then(mapUsuario);
}
