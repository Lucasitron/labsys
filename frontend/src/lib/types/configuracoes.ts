import type { Module } from './auth';

export type Nivel = 0 | 1 | 2 | 3 | 4;

export type SituacaoUsuario = 'Ativo' | 'Pendente' | 'Desativado';

export interface Usuario {
	id: string;
	nome: string;
	email: string;
	telefone?: string | null;
	nivel: Nivel;
	situacao: SituacaoUsuario;
	foto?: string | null;
	criadoEm: string;
}

export interface Perfil {
	nome: string;
	email: string;
	telefone?: string | null;
	funcao?: string | null;
	avatar?: string | null;
}

export type PermissaoNivel = 'Ver' | 'Editar' | 'Nenhum';

export type PermissaoMatriz = Record<Module, Record<Nivel, PermissaoNivel>>;

export interface ParametroSistema {
	identidade: {
		nome: string;
		logo?: string | null;
	};
	cadenciaChecklist5S: string;
	cadenciaAuditoria5S?: string | null;
	tokens: TokenIntegracao[];
}

export interface TokenIntegracao {
	id: string;
	nome: string;
	prefixo: string;
	criadoEm: string;
	ultimoUso?: string | null;
	revogado: boolean;
}

export interface Paginacao {
	page: number;
	pageSize: number;
	totalItems: number;
	totalPages: number;
}

export interface Paginado<T> {
	dados: T[];
	paginacao: Paginacao;
}