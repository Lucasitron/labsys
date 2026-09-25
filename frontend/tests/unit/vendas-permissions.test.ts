import { describe, expect, it } from 'vitest';
import {
	EDIT_RULES,
	canDecideVendas,
	canEditVendas,
	isResponsavelVendas
} from '$lib/utils/permissions';
import type { User } from '$lib/types/auth';

function usuario(overrides: Partial<User> = {}): User {
	return {
		id: 'u1',
		username: 'maria',
		name: 'Maria Santos',
		email: 'maria@fablab.org',
		role: 2,
		...overrides
	};
}

describe('vendas-permissions — canEditVendas (criador edita, outro não)', () => {
	it('admin edita qualquer registro', () => {
		expect(canEditVendas(usuario({ role: 0 }), { createdBy: 'outra-pessoa' })).toBe(true);
	});

	it('admin edita mesmo sem createdBy no registro', () => {
		expect(canEditVendas(usuario({ role: 0 }), {})).toBe(true);
	});

	it('criador (createdBy === id) edita', () => {
		expect(canEditVendas(usuario({ id: 'u1' }), { createdBy: 'u1' })).toBe(true);
	});

	it('outra pessoa NÃO edita', () => {
		expect(canEditVendas(usuario({ id: 'u1' }), { createdBy: 'u2' })).toBe(false);
	});

	it('sem createdBy, não-admin NÃO edita', () => {
		expect(canEditVendas(usuario({ id: 'u1' }), {})).toBe(false);
	});

	it('negado para usuário nulo', () => {
		expect(canEditVendas(null, { createdBy: 'u1' })).toBe(false);
	});
});

describe('vendas-permissions — canDecideVendas (Admin + responsável)', () => {
	it('admin decide', () => {
		expect(canDecideVendas(usuario({ role: 0 }))).toBe(true);
	});

	it('responsável de vendas decide', () => {
		const u = usuario({ role: 2, responsibilities: { vendas: ['crm'] } });
		expect(canDecideVendas(u)).toBe(true);
	});

	it('criador comum (sem responsabilidade) NÃO decide', () => {
		expect(canDecideVendas(usuario({ role: 2 }))).toBe(false);
	});

	it('negado para usuário nulo', () => {
		expect(canDecideVendas(null)).toBe(false);
	});
});

describe('vendas-permissions — isResponsavelVendas', () => {
	it('admin é responsável por vendas', () => {
		expect(isResponsavelVendas(usuario({ role: 0 }))).toBe(true);
	});

	it('com responsibilities.vendas é responsável', () => {
		const u = usuario({ role: 1, responsibilities: { vendas: ['crm'] } });
		expect(isResponsavelVendas(u)).toBe(true);
	});

	it('sem responsibilities NÃO é responsável', () => {
		expect(isResponsavelVendas(usuario({ role: 1 }))).toBe(false);
	});

	it('recrutando (role 4) nunca é responsável, mesmo com flag', () => {
		const u = usuario({ role: 4, responsibilities: { vendas: ['crm'] } });
		expect(isResponsavelVendas(u)).toBe(false);
	});

	it('negado para usuário nulo', () => {
		expect(isResponsavelVendas(null)).toBe(false);
	});
});

describe('vendas-permissions — EDIT_RULES.vendas intacto', () => {
	it('EDIT_RULES.vendas permanece [0]', () => {
		expect(EDIT_RULES.vendas).toEqual([0]);
	});
});
