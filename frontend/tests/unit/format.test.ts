import { describe, expect, it } from 'vitest';
import { firstName, formatNumber, greeting, initials, relativeTime } from '$lib/utils/format';

describe('format', () => {
	describe('greeting', () => {
		it('retorna "Bom dia" antes do meio-dia', () => {
			expect(greeting(new Date(2026, 0, 1, 9))).toBe('Bom dia');
		});

		it('retorna "Boa tarde" entre 12h e 18h', () => {
			expect(greeting(new Date(2026, 0, 1, 15))).toBe('Boa tarde');
		});

		it('retorna "Boa noite" após 18h', () => {
			expect(greeting(new Date(2026, 0, 1, 21))).toBe('Boa noite');
		});
	});

	describe('firstName', () => {
		it('extrai o primeiro nome', () => {
			expect(firstName('João da Silva')).toBe('João');
		});

		it('retorna string vazia para nome vazio', () => {
			expect(firstName('   ')).toBe('');
		});
	});

	describe('initials', () => {
		it('usa inicial do primeiro e último nome', () => {
			expect(initials('João Silva')).toBe('JS');
		});

		it('usa até duas letras quando nome único', () => {
			expect(initials('João')).toBe('JO');
		});
	});

	describe('relativeTime', () => {
		const now = new Date('2026-09-18T12:00:00Z');

		it('formata minutos no passado', () => {
			expect(relativeTime('2026-09-18T11:48:00Z', now)).toBe('há 12 minutos');
		});

		it('formata horas no passado', () => {
			expect(relativeTime('2026-09-18T10:00:00Z', now)).toBe('há 2 horas');
		});

		it('formata ontem', () => {
			expect(relativeTime('2026-09-17T12:00:00Z', now)).toBe('ontem');
		});
	});

	describe('formatNumber', () => {
		it('usa separador pt-BR', () => {
			expect(formatNumber(1234.5)).toBe('1.234,5');
		});
	});
});