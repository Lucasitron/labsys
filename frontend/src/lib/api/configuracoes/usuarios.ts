import { bearer } from '$lib/api/bearer';
import { apiFetch } from '$lib/api/client';
import type { Nivel, Paginado, SituacaoUsuario, Usuario } from '$lib/types/configuracoes';

export interface UsuarioPayload {
	nome: string;
	email: string;
	nivel: Nivel;
	situacao?: SituacaoUsuario;
	idUser?: number;
	senha?: string;
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

function montarQuery(filtros: UsuariosFiltros): string {
	const search = new URLSearchParams();
	// Back pagina de 0; a UI pagina de 1.
	if (filtros.page !== undefined) search.set('page', String(Math.max(0, filtros.page - 1)));
	if (filtros.pageSize !== undefined) search.set('size', String(filtros.pageSize));
	if (filtros.search) search.set('search', filtros.search);
	if (filtros.nivel !== undefined) search.set('nivel', String(filtros.nivel));
	if (filtros.situacao) search.set('situacao', SITUACAO_PARA_UPPER[filtros.situacao]);
	const qs = search.toString();
	return qs ? `?${qs}` : '';
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
		`/usuarios${montarQuery(filtros)}`,
		{ headers: bearer() },
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

export function criarUsuario(payload: UsuarioPayload): Promise<Usuario> {
	if (payload.idUser === undefined || !payload.senha) {
		return Promise.reject(
			new Error('Selecione a pessoa do RH vinculada para criar o acesso.')
		);
	}
	return apiFetch<RawUsuario>('/usuarios', {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify({
			idUser: payload.idUser,
			email: payload.email,
			nomeUsuario: payload.nome,
			senha: payload.senha,
			nivel: payload.nivel,
			...(payload.situacao ? { situacao: SITUACAO_PARA_UPPER[payload.situacao] } : {})
		})
	}).then(mapUsuario);
}

export function atualizarUsuario(id: string, payload: UsuarioPayload): Promise<Usuario> {
	return apiFetch<RawUsuario>(`/usuarios/${id}`, {
		method: 'PUT',
		headers: bearer(),
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
		headers: bearer(),
		body: JSON.stringify({ situacao: SITUACAO_PARA_UPPER[situacao] })
	}).then(mapUsuario);
}
