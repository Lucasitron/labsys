import type { Module, Role, User } from '$lib/types/auth';

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

export function canEdit(user: User | null, module: Module): boolean {
	if (!user) return false;
	if (user.role === 0) return true;
	if (user.roles?.[module] === 'edit') return true;

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