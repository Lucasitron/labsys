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
	isResponsavelAtribuido
} from '$lib/utils/permissions';
import type { Module, User } from '$lib/types/auth';

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

		it('roles 1..3 com grant "view" ou "edit" veem o 5S', () => {
			for (const role of [1, 2, 3] as const) {
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
	});

	describe('canSeeAdvertencias', () => {
		it('admin vê advertências', () => {
			expect(canSeeAdvertencias(user({ role: 0 }))).toBe(true);
		});

		it('não-admin com roles.producao === "edit" vê advertências', () => {
			const u = user({ role: 2, roles: { producao: 'edit' } });
			expect(canSeeAdvertencias(u)).toBe(true);
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
});