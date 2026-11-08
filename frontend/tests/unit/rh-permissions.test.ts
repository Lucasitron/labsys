import { describe, expect, it } from 'vitest';
import {
	EDIT_RULES,
	canEditRH,
	canSeeNiveis,
	isInstrutor,
	isResponsavelRH,
	isTutor
} from '$lib/utils/permissions';
import { menuItems } from '$lib/config/menu';
import type { User } from '$lib/types/auth';

function usuario(overrides: Partial<User> = {}): User {
	return {
		id: 'u1',
		username: 'joao',
		name: 'João Silva',
		email: 'joao@fablab.org',
		role: 2,
		...overrides
	};
}

describe('rh-permissions — isResponsavelRH', () => {
	it('admin (role 0) é responsável por RH', () => {
		expect(isResponsavelRH(usuario({ role: 0 }))).toBe(true);
	});

	it('bolsista sem responsabilidade NÃO é responsável por RH', () => {
		expect(isResponsavelRH(usuario({ role: 1 }))).toBe(false);
	});

	it('bolsista com responsibilities.rh é responsável por RH', () => {
		const u = usuario({ role: 1, responsibilities: { rh: ['g-01'] } });
		expect(isResponsavelRH(u)).toBe(true);
	});

	it('recrutando (role 4) nunca é responsável por RH, mesmo com flag', () => {
		const u = usuario({ role: 4, responsibilities: { rh: ['g-01'] } });
		expect(isResponsavelRH(u)).toBe(false);
	});
});

describe('rh-permissions — canEditRH', () => {
	it('admin pode editar RH', () => {
		expect(canEditRH(usuario({ role: 0 }))).toBe(true);
	});

	it('bolsista responsável pode editar RH', () => {
		const u = usuario({ role: 1, responsibilities: { rh: ['g-01'] } });
		expect(canEditRH(u)).toBe(true);
	});

	it('bolsista comum (sem responsabilidade) NÃO edita RH', () => {
		expect(canEditRH(usuario({ role: 1 }))).toBe(false);
	});

	it('estagiário (role 3) NÃO edita RH, mesmo responsável', () => {
		const u = usuario({ role: 3, responsibilities: { rh: ['g-01'] } });
		expect(canEditRH(u)).toBe(false);
	});

	it('negado para usuário nulo', () => {
		expect(canEditRH(null)).toBe(false);
	});
});

describe('rh-permissions — canSeeNiveis (Admin-only)', () => {
	it('admin vê Níveis & acesso', () => {
		expect(canSeeNiveis(usuario({ role: 0 }))).toBe(true);
	});

	it('bolsista responsável NÃO vê Níveis & acesso', () => {
		const u = usuario({ role: 1, responsibilities: { rh: ['g-01'] } });
		expect(canSeeNiveis(u)).toBe(false);
	});

	it('negado para usuário nulo', () => {
		expect(canSeeNiveis(null)).toBe(false);
	});
});

describe('rh-permissions — isTutor/isInstrutor distintos', () => {
	it('tutor de grupo (sem flag instrutor) é tutor mas não instrutor', () => {
		const u = usuario({ role: 1, responsibilities: { rh: ['g-01'] } });
		expect(isTutor(u)).toBe(true);
		expect(isInstrutor(u)).toBe(false);
	});

	it('mesma pessoa pode ser tutora e instrutora (flags independentes)', () => {
		const u = usuario({ role: 1, responsibilities: { rh: ['g-01', 'instrutor-laser'] } });
		expect(isTutor(u)).toBe(true);
		expect(isInstrutor(u)).toBe(true);
	});

	it('sem responsabilidade não é nem tutor nem instrutor', () => {
		const u = usuario({ role: 2 });
		expect(isTutor(u)).toBe(false);
		expect(isInstrutor(u)).toBe(false);
	});

	it('admin é tutor e instrutor', () => {
		const u = usuario({ role: 0 });
		expect(isTutor(u)).toBe(true);
		expect(isInstrutor(u)).toBe(true);
	});
});

describe('rh-permissions — EDIT_RULES.rh intacto', () => {
	it('EDIT_RULES.rh permanece [0,1,2]', () => {
		expect(EDIT_RULES.rh).toEqual([0, 1, 2]);
	});
});

describe('rh-permissions — menu Pessoas & RH (hide real de Níveis)', () => {
	const item = menuItems.find((m) => m.path === '/pessoas');
	const niveis = item?.children?.find((c) => c.path === '/pessoas/niveis');

	it('raiz preserva module/icon/path e tem 5 filhos', () => {
		expect(item?.module).toBe('rh');
		expect(item?.icon).toBe('rh');
		expect(item?.children).toHaveLength(5);
	});

	it('filho Níveis & acesso usa canSee = Admin-only', () => {
		expect(niveis?.label).toBe('Níveis & acesso');
		expect(niveis?.canSee?.(usuario({ role: 0 }))).toBe(true);
		expect(niveis?.canSee?.(usuario({ role: 1, responsibilities: { rh: ['g-01'] } }))).toBe(false);
		expect(niveis?.canSee?.(null)).toBe(false);
	});

	it('Níveis omitido para não-admin (hide real no menu visível)', () => {
		const naoAdmin = usuario({ role: 2, responsibilities: { rh: ['g-01'] } });
		const visiveis = (item?.children ?? []).filter((c) => (c.canSee ? c.canSee(naoAdmin) : true));
		expect(visiveis.map((c) => c.path)).not.toContain('/pessoas/niveis');
		expect(visiveis).toHaveLength(4);
	});

	it('Níveis visível para admin', () => {
		const admin = usuario({ role: 0 });
		const visiveis = (item?.children ?? []).filter((c) => (c.canSee ? c.canSee(admin) : true));
		expect(visiveis.map((c) => c.path)).toContain('/pessoas/niveis');
		expect(visiveis).toHaveLength(5);
	});
});
