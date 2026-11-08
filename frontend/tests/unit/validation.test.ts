import { describe, expect, it } from 'vitest';
import { isEmail } from '$lib/utils/validation';

describe('validation', () => {
	describe('isEmail', () => {
		it('retorna false para vazio', () => {
			expect(isEmail('')).toBe(false);
		});

		it('retorna false para só espaços', () => {
			expect(isEmail('   ')).toBe(false);
		});

		it('retorna false sem @', () => {
			expect(isEmail('usuariofablab.org')).toBe(false);
		});

		it('retorna false sem domínio', () => {
			expect(isEmail('usuario@')).toBe(false);
		});

		it('retorna true para e-mail válido', () => {
			expect(isEmail('usuario@fablab.org')).toBe(true);
		});

		it('tolera espaços nas pontas', () => {
			expect(isEmail('  usuario@fablab.org  ')).toBe(true);
		});
	});
});
