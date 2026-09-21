import { describe, expect, it } from 'vitest';
import {
	EDIT_RULES,
	VIEW_RULES,
	canAuditar5S,
	canEdit,
	canEditProducao,
	canSee5S,
	canSeeAdvertencias,
	canSeeLoans,
	canSeeMachines,
	canView,
	canViewConfiguracoes,
	hasPermissao,
	isAdmin,
	isResponsavelAtribuido
} from '$lib/utils/permissions';
import type { Module, User } from '$lib/types/auth';
import type { Nivel, PermissaoMatriz, PermissaoNivel } from '$lib/types/configuracoes';

function user(overrides: Partial<User> = {}): User {
	return {
		id: 'u1',
		username: 'joao',
		name: 'João Silva',
		email: 'joao@fablab.org',
		role: 2,
		...overrides
	};
}

function matrizCom(modulo: Module, nivel: Nivel, valor: PermissaoNivel): PermissaoMatriz {
	const modulos: Module[] = [
		'dashboard',
		'rh',
		'estoque',
		'vendas',
		'financeiro',
		'producao',
		'notificacoes',
		'configuracoes'
	];
	const niveis: Nivel[] = [0, 1, 2, 3, 4];
	const mat = {} as PermissaoMatriz;
	for (const m of modulos) {
		mat[m] = {} as Record<Nivel, PermissaoNivel>;
		for (const n of niveis) {
			mat[m][n] = m === modulo && n === nivel ? valor : 'Nenhum';
		}
	}
	return mat;
}

describe('permissions', () => {
	describe('canView', () => {
		it('negado para usuário nulo', () => {
			expect(canView(null, 'estoque')).toBe(false);
		});

		it('admin vê qualquer módulo', () => {
			expect(canView(user({ role: 0 }), 'financeiro')).toBe(true);
		});

		it('usa as regras por papel quando não há roles explícitas', () => {
			expect(canView(user({ role: 2 }), 'estoque')).toBe(true);
			expect(canView(user({ role: 4 }), 'estoque')).toBe(false);
			expect(canView(user({ role: 3 }), 'financeiro')).toBe(false);
		});

		it('roles explícitas sobrepõem as regras por papel', () => {
			const u = user({ role: 4, roles: { estoque: 'view' } });
			expect(canView(u, 'estoque')).toBe(true);
		});

		it('roles explícitas nulas negam acesso', () => {
			const u = user({ role: 1, roles: { rh: null } });
			expect(canView(u, 'rh')).toBe(false);
		});
	});

	describe('canEdit', () => {
		it('admin pode editar qualquer módulo', () => {
			expect(canEdit(user({ role: 0 }), 'financeiro')).toBe(true);
		});

		it('negado por papel sem permissão de edição', () => {
			expect(canEdit(user({ role: 2 }), 'estoque')).toBe(false);
			expect(canEdit(user({ role: 1 }), 'financeiro')).toBe(false);
		});

		it('roles explícitas "edit" concedem edição', () => {
			const u = user({ role: 2, roles: { estoque: 'edit' } });
			expect(canEdit(u, 'estoque')).toBe(true);
		});
	});

	describe('canSeeLoans', () => {
		it('admin vê empréstimos', () => {
			expect(canSeeLoans(user({ role: 0 }))).toBe(true);
		});

		it('responsável por estoque vê empréstimos', () => {
			expect(canSeeLoans(user({ responsibilities: { estoque: ['cat_3d'] } }))).toBe(true);
		});

		it('sem responsabilidade não vê empréstimos', () => {
			expect(canSeeLoans(user())).toBe(false);
		});

		it('negado para usuário nulo', () => {
			expect(canSeeLoans(null)).toBe(false);
		});
	});

	describe('canSeeMachines', () => {
		it('admin vê máquinas', () => {
			expect(canSeeMachines(user({ role: 0 }))).toBe(true);
		});

		it('responsável por produção vê máquinas', () => {
			expect(canSeeMachines(user({ responsibilities: { producao: ['laser'] } }))).toBe(true);
		});

		it('sem responsabilidade não vê máquinas', () => {
			expect(canSeeMachines(user())).toBe(false);
		});

		it('negado para usuário nulo', () => {
			expect(canSeeMachines(null)).toBe(false);
		});
	});

	describe('canSee5S', () => {
		it('admin (role 0) sempre vê o 5S', () => {
			expect(canSee5S(user({ role: 0 }))).toBe(true);
		});

		it('Recrutando (role 4) não vê o 5S mesmo com grant de produção', () => {
			const u = user({ role: 4, roles: { producao: 'view' } });
			expect(canSee5S(u)).toBe(false);
		});

		it('Estagiário (role 3) não vê o 5S mesmo com grant', () => {
			expect(canSee5S(user({ role: 3, roles: { producao: 'view' } }))).toBe(false);
			expect(canSee5S(user({ role: 3, roles: { producao: 'edit' } }))).toBe(false);
		});

		it('roles 1..2 com grant "view" ou "edit" veem o 5S', () => {
			for (const role of [1, 2] as const) {
				expect(canSee5S(user({ role, roles: { producao: 'view' } }))).toBe(true);
				expect(canSee5S(user({ role, roles: { producao: 'edit' } }))).toBe(true);
			}
		});

		it('sem grant de produção não vê o 5S', () => {
			expect(canSee5S(user({ role: 2 }))).toBe(false);
			expect(canSee5S(user({ role: 1 }))).toBe(false);
			expect(canSee5S(user({ role: 3 }))).toBe(false);
		});

		it('roles.producao === null nega o 5S mesmo para role 1..3', () => {
			expect(canSee5S(user({ role: 1, roles: { producao: null } }))).toBe(false);
		});

		it('negado para usuário nulo', () => {
			expect(canSee5S(null)).toBe(false);
		});
	});

	describe('isResponsavelAtribuido', () => {
		it('admin sempre é considerado responsável', () => {
			expect(isResponsavelAtribuido(user({ role: 0 }), { responsavelId: 'qualquer' })).toBe(true);
			expect(isResponsavelAtribuido(user({ role: 0 }), {})).toBe(true);
		});

		it('true quando o id está em responsibilities.producao', () => {
			const u = user({ responsibilities: { producao: ['cat_3d', 'laser'] } });
			expect(isResponsavelAtribuido(u, { responsavelId: 'laser' })).toBe(true);
		});

		it('false quando o id não está em responsibilities.producao', () => {
			const u = user({ responsibilities: { producao: ['cat_3d'] } });
			expect(isResponsavelAtribuido(u, { responsavelId: 'laser' })).toBe(false);
		});

		it('false quando o recurso não tem responsavelId', () => {
			expect(isResponsavelAtribuido(user(), {})).toBe(false);
			expect(isResponsavelAtribuido(user(), { responsavelId: undefined })).toBe(false);
		});

		it('negado para usuário nulo', () => {
			expect(isResponsavelAtribuido(null, { responsavelId: 'laser' })).toBe(false);
		});
	});

	describe('canAuditar5S', () => {
		it('admin sempre pode auditar', () => {
			expect(canAuditar5S(user({ role: 0 }))).toBe(true);
		});

		it('auditor nomeado (responsável) pode auditar', () => {
			const u = user({ responsibilities: { producao: ['auditor_1'] } });
			expect(canAuditar5S(u, 'auditor_1')).toBe(true);
		});

		it('auditor nomeado fora da responsabilidade não pode auditar', () => {
			const u = user({ responsibilities: { producao: ['outro'] } });
			expect(canAuditar5S(u, 'auditor_1')).toBe(false);
		});

		it('sem auditor nomeado, roles 1 e 2 podem auditar (fallback)', () => {
			expect(canAuditar5S(user({ role: 1 }))).toBe(true);
			expect(canAuditar5S(user({ role: 2 }))).toBe(true);
		});

		it('sem auditor nomeado, roles 3 e 4 não podem auditar', () => {
			expect(canAuditar5S(user({ role: 3 }))).toBe(false);
			expect(canAuditar5S(user({ role: 4 }))).toBe(false);
		});

		it('Estagiário (role 3) nunca audita, mesmo nomeado auditor', () => {
			const u = user({ role: 3, responsibilities: { producao: ['auditor_1'] } });
			expect(canAuditar5S(u, 'auditor_1')).toBe(false);
		});

		it('Recrutando (role 4) nunca audita, mesmo nomeado auditor', () => {
			const u = user({ role: 4, responsibilities: { producao: ['auditor_1'] } });
			expect(canAuditar5S(u, 'auditor_1')).toBe(false);
		});
	});

	describe('canSeeAdvertencias', () => {
		it('admin vê advertências', () => {
			expect(canSeeAdvertencias(user({ role: 0 }))).toBe(true);
		});

		it('não-admin com roles.producao === "edit" NÃO vê advertências (Admin-only)', () => {
			const u = user({ role: 2, roles: { producao: 'edit' } });
			expect(canSeeAdvertencias(u)).toBe(false);
		});

		it('Bolsista (role 1) com grant "edit" não vê advertências (Admin-only)', () => {
			const u = user({ role: 1, roles: { producao: 'edit' } });
			expect(canSeeAdvertencias(u)).toBe(false);
		});

		it('Recrutando (role 4) não vê advertências', () => {
			expect(canSeeAdvertencias(user({ role: 4 }))).toBe(false);
		});

		it('role 2 sem grant "edit" não vê advertências', () => {
			expect(canSeeAdvertencias(user({ role: 2 }))).toBe(false);
		});

		it('negado para usuário nulo', () => {
			expect(canSeeAdvertencias(null)).toBe(false);
		});
	});

	describe('canEditProducao', () => {
		it('admin pode editar produção', () => {
			expect(canEditProducao(user({ role: 0 }))).toBe(true);
		});

		it('responsável atribuído a um recurso pode editá-lo', () => {
			const u = user({ responsibilities: { producao: ['laser'] } });
			expect(canEditProducao(u, { responsavelId: 'laser' })).toBe(true);
		});

		it('recurso não atribuído não pode ser editado', () => {
			const u = user({ responsibilities: { producao: ['cat_3d'] } });
			expect(canEditProducao(u, { responsavelId: 'laser' })).toBe(false);
		});

		it('sem recurso e sem regra de edição, role 1 não edita (EDIT_RULES.producao = [0])', () => {
			expect(canEditProducao(user({ role: 1 }))).toBe(false);
			expect(canEditProducao(user({ role: 2 }))).toBe(false);
		});

		it('negado para usuário nulo', () => {
			expect(canEditProducao(null)).toBe(false);
		});
	});

	describe('matrix RBAC', () => {
		it('VIEW_RULES permanece como baseline', () => {
			const baseline: Record<Module, readonly number[]> = {
				dashboard: [0, 1, 2, 3, 4],
				rh: [0, 1, 2, 3, 4],
				estoque: [0, 1, 2, 3],
				vendas: [0, 1, 2, 3],
				financeiro: [0],
				producao: [0, 1, 2, 3],
				notificacoes: [0, 1, 2, 3, 4],
				configuracoes: [0, 1, 2, 3, 4]
			};
			expect(VIEW_RULES).toEqual(baseline);
		});

		it('EDIT_RULES permanece como baseline', () => {
			const baseline: Record<Module, readonly number[]> = {
				dashboard: [],
				rh: [0, 1, 2],
				estoque: [0],
				vendas: [0],
				financeiro: [0],
				producao: [0],
				notificacoes: [0],
				configuracoes: [0]
			};
			expect(EDIT_RULES).toEqual(baseline);
		});

		it('producao é view para roles 1..3 e edit somente para admin', () => {
			expect(VIEW_RULES.producao).toEqual([0, 1, 2, 3]);
			expect(EDIT_RULES.producao).toEqual([0]);
		});
	});

	describe('isAdmin', () => {
		it('admin (role 0) é true', () => {
			expect(isAdmin(user({ role: 0 }))).toBe(true);
		});

		it('roles 1..4 não são admin', () => {
			for (const role of [1, 2, 3, 4] as const) {
				expect(isAdmin(user({ role }))).toBe(false);
			}
		});

		it('negado para usuário nulo', () => {
			expect(isAdmin(null)).toBe(false);
		});
	});

	describe('canViewConfiguracoes', () => {
		it('admin vê o módulo configurações', () => {
			expect(canViewConfiguracoes(user({ role: 0 }))).toBe(true);
		});

		it('roles 1..4 não veem configurações (Admin-only)', () => {
			for (const role of [1, 2, 3, 4] as const) {
				expect(canViewConfiguracoes(user({ role }))).toBe(false);
			}
		});
	});

	describe('hasPermissao', () => {
		it('admin sempre tem permissão para Ver e Editar', () => {
			const admin = user({ role: 0 });
			expect(hasPermissao(admin, 'configuracoes', 'Ver')).toBe(true);
			expect(hasPermissao(admin, 'configuracoes', 'Editar')).toBe(true);
		});

		it('acao "Nenhum" é sempre negada, mesmo para admin', () => {
			expect(hasPermissao(user({ role: 0 }), 'configuracoes', 'Nenhum')).toBe(false);
			expect(hasPermissao(user({ role: 2 }), 'configuracoes', 'Nenhum')).toBe(false);
		});

		it('com matriz, célula "Ver" concede Ver mas não Editar', () => {
			const mat = matrizCom('configuracoes', 2, 'Ver');
			const u = user({ role: 2 });
			expect(hasPermissao(u, 'configuracoes', 'Ver', mat)).toBe(true);
			expect(hasPermissao(u, 'configuracoes', 'Editar', mat)).toBe(false);
		});

		it('com matriz, célula "Editar" concede Ver e Editar', () => {
			const mat = matrizCom('configuracoes', 3, 'Editar');
			const u = user({ role: 3 });
			expect(hasPermissao(u, 'configuracoes', 'Ver', mat)).toBe(true);
			expect(hasPermissao(u, 'configuracoes', 'Editar', mat)).toBe(true);
		});

		it('com matriz, célula "Nenhum" nega mesmo com grant de roles', () => {
			const mat = matrizCom('configuracoes', 2, 'Nenhum');
			const u = user({ role: 2, roles: { configuracoes: 'view' } });
			expect(hasPermissao(u, 'configuracoes', 'Ver', mat)).toBe(false);
		});

		it('sem matriz, fallback "Ver" usa canView e "Editar" usa canEdit', () => {
			expect(hasPermissao(user({ role: 4 }), 'configuracoes', 'Ver')).toBe(true);
			expect(hasPermissao(user({ role: 4 }), 'configuracoes', 'Editar')).toBe(false);
			expect(
				hasPermissao(user({ role: 2, roles: { configuracoes: 'edit' } }), 'configuracoes', 'Editar')
			).toBe(true);
		});

		it('negado para usuário nulo', () => {
			expect(hasPermissao(null, 'configuracoes', 'Ver')).toBe(false);
		});
	});
});