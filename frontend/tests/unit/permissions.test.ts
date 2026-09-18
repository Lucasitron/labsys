import { describe, expect, it } from 'vitest';
import { canEdit, canSeeLoans, canSeeMachines, canView } from '$lib/utils/permissions';
import type { User } from '$lib/types/auth';

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
});