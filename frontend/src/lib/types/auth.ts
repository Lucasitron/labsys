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

export interface LoginResponse {
	token: string;
	expiresIn: number;
	user: User;
}