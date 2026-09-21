import type { Module, Role, User } from '$lib/types/auth';
import type { PermissaoMatriz, PermissaoNivel } from '$lib/types/configuracoes';

export const VIEW_RULES: Record<Module, readonly Role[]> = {
	dashboard: [0, 1, 2, 3, 4],
	rh: [0, 1, 2, 3, 4],
	estoque: [0, 1, 2, 3],
	vendas: [0, 1, 2, 3],
	financeiro: [0],
	producao: [0, 1, 2, 3],
	notificacoes: [0, 1, 2, 3, 4],
	configuracoes: [0, 1, 2, 3, 4]
};

export const EDIT_RULES: Record<Module, readonly Role[]> = {
	dashboard: [],
	rh: [0, 1, 2],
	estoque: [0],
	vendas: [0],
	financeiro: [0],
	producao: [0],
	notificacoes: [0],
	configuracoes: [0]
};

export function canView(user: User | null, module: Module): boolean {
	if (!user) return false;
	if (user.role === 0) return true;

	if (user.roles) {
		const access = user.roles[module];
		if (access === 'view' || access === 'edit') return true;
		if (access === null) return false;
	}

	return VIEW_RULES[module].includes(user.role);
}

export function isResponsavelEstoque(user: User | null): boolean {
	if (!user) return false;
	if (user.role === 0) return true;
	if (user.role === 4) return false;
	return (user.responsibilities?.estoque?.length ?? 0) > 0;
}

export function canVerFornecedores(user: User | null): boolean {
	if (!user) return false;
	return isAdmin(user) || user.role === 1;
}

export function canEdit(user: User | null, module: Module): boolean {
	if (!user) return false;
	if (user.role === 0) return true;
	if (user.roles?.[module] === 'edit') return true;
	if (module === 'estoque' && isResponsavelEstoque(user)) return true;

	return EDIT_RULES[module].includes(user.role);
}

export function canSeeLoans(user: User | null): boolean {
	if (!user) return false;
	return user.role === 0 || (user.responsibilities?.estoque?.length ?? 0) > 0;
}

export function canSeeMachines(user: User | null): boolean {
	if (!user) return false;
	return user.role === 0 || (user.responsibilities?.producao?.length ?? 0) > 0;
}

export function canSee5S(user: User | null): boolean {
	if (!user) return false;
	if (user.role === 0) return true;
	if (user.roles?.producao === null) return false;
	const access = user.roles?.producao;
	return (access === 'view' || access === 'edit') && user.role >= 1 && user.role <= 2;
}

export function isResponsavelAtribuido(
	user: User | null,
	recurso: { responsavelId?: string }
): boolean {
	if (!user) return false;
	if (user.role === 0) return true;
	if (!recurso.responsavelId) return false;
	return user.responsibilities?.producao?.includes(recurso.responsavelId) ?? false;
}

export function canAuditar5S(user: User | null, auditorId?: string): boolean {
	if (!user) return false;
	if (user.role === 0) return true;
	if (user.role === 3 || user.role === 4) return false;
	if (auditorId) return isResponsavelAtribuido(user, { responsavelId: auditorId });
	return user.role === 1 || user.role === 2;
}

export function canSeeAdvertencias(user: User | null): boolean {
	if (!user) return false;
	return user.role === 0;
}

export function canEditProducao(
	user: User | null,
	recurso?: { responsavelId?: string }
): boolean {
	if (!user) return false;
	if (canEdit(user, 'producao')) return true;
	if (recurso) return isResponsavelAtribuido(user, recurso);
	return false;
}

export function isAdmin(user: User | null): boolean {
	return user?.role === 0;
}

export function canViewConfiguracoes(user: User | null): boolean {
	return isAdmin(user);
}

export function hasPermissao(
	user: User | null,
	modulo: Module,
	acao: PermissaoNivel,
	matriz?: PermissaoMatriz
): boolean {
	if (!user) return false;
	if (acao === 'Nenhum') return false;
	if (isAdmin(user)) return true;

	if (matriz) {
		const celula = matriz[modulo]?.[user.role];
		if (acao === 'Editar') return celula === 'Editar';
		return celula === 'Editar' || celula === 'Ver';
	}

	return acao === 'Editar' ? canEdit(user, modulo) : canView(user, modulo);
}