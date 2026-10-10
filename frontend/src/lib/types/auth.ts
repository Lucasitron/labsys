export type Role = 0 | 1 | 2 | 3 | 4;

export type Module =
	| 'dashboard'
	| 'rh'
	| 'estoque'
	| 'vendas'
	| 'financeiro'
	| 'producao'
	| 'notificacoes'
	| 'configuracoes';

export type ModuleAccess = 'view' | 'edit' | null;

export interface User {
	id: string;
	username: string;
	name: string;
	role: Role;
	email: string;
	roles?: Partial<Record<Module, ModuleAccess>>;
	responsibilities?: Partial<Record<Module, string[]>>;
}

export interface LoginRequest {
	username: string;
	password: string;
}

// Corpo real exigido pelo backend Laravel (POST /api/auth/login):
// email OU nomeUsuario obrigatório + senha.
export interface BackendLoginBody {
	email?: string;
	nomeUsuario?: string;
	senha: string;
}

export type BackendRoleName = 'ADMIN' | 'BOLSISTA' | 'VOLUNTARIO' | 'ESTAGIARIO' | 'RECRUTANDO';

// Resposta real do backend Laravel (POST /api/auth/login).
export interface LoginResponse {
	accessToken: string;
	refreshToken: string;
	tokenType: 'Bearer';
	expiresIn: number;
	idUser: number;
	role: BackendRoleName;
	setor: string | null;
	nomeUsuario: string;
}

export interface MePermission {
	role: Role;
	label: string;
	active: boolean;
}

// Resposta real do backend Laravel (GET /api/auth/me).
export interface MeResponse {
	id: number;
	idUser: number;
	email: string;
	nomeUsuario: string;
	setor: string | null;
	permissions: MePermission[];
}

// Resposta real do backend Laravel (POST /api/auth/refresh).
export interface RefreshResponse {
	accessToken: string;
	refreshToken: string;
	tokenType: 'Bearer';
	expiresIn: number;
}