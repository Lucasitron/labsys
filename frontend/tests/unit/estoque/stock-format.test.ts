import { describe, expect, it } from 'vitest';
import { daysFromToday, fmtDate, fmtDateTime, fmtMoney, fmtQty, toDateInputValue } from '$lib/utils/stock-format';

const NBSP = '\u00A0';
const clean = (s: string) => s.replace(new RegExp(NBSP, 'g'), ' ');

describe('estoque/stock-format', () => {
	describe('fmtMoney', () => {
		it('retorna "—" para null / undefined / NaN', () => {
			expect(fmtMoney(null)).toBe('—');
			expect(fmtMoney(undefined)).toBe('—');
			expect(fmtMoney(Number.NaN)).toBe('—');
		});

		it('formata zero como R$ 0,00', () => {
			expect(clean(fmtMoney(0))).toBe('R$ 0,00');
		});

		it('formata valor positivo em pt-BR com duas casas', () => {
			expect(clean(fmtMoney(1234.5))).toBe('R$ 1.234,50');
		});

		it('formata valor negativo', () => {
			expect(clean(fmtMoney(-42))).toBe('-R$ 42,00');
		});
	});

	describe('fmtQty', () => {
		it('formata número inteiro sem unidade', () => {
			expect(fmtQty(1000)).toBe('1.000');
		});

		it('formata com unidade', () => {
			expect(fmtQty(2.5, 'un')).toBe('2,5 un');
		});

		it('respeita máximo de 2 casas decimais', () => {
			expect(fmtQty(1.2345, 'kg')).toBe('1,23 kg');
		});

		it('trata zero', () => {
			expect(fmtQty(0, 'L')).toBe('0 L');
		});
	});

	describe('fmtDate', () => {
		it('retorna "—" para null / vazio', () => {
			expect(fmtDate(null)).toBe('—');
			expect(fmtDate('')).toBe('—');
		});

		it('formata string YYYY-MM-DD simples como DD/MM/YYYY', () => {
			expect(fmtDate('2026-02-03')).toBe('03/02/2026');
		});

		it('formata ISO datetime usando Intl pt-BR', () => {
			const input = '2026-09-21T10:30:00Z';
			const expected = new Intl.DateTimeFormat('pt-BR').format(new Date(input));
			expect(fmtDate(input)).toBe(expected);
		});

		it('retorna a string original se a data for inválida', () => {
			expect(fmtDate('not-a-date')).toBe('not-a-date');
		});
	});

	describe('fmtDateTime', () => {
		it('retorna "—" para null', () => {
			expect(fmtDateTime(null)).toBe('—');
		});

		it('formata ISO completo com horas/minutos via Intl pt-BR', () => {
			const input = '2026-09-21T14:30:00Z';
			const expected = new Intl.DateTimeFormat('pt-BR', {
				day: '2-digit',
				month: '2-digit',
				year: 'numeric',
				hour: '2-digit',
				minute: '2-digit'
			}).format(new Date(input));
			expect(fmtDateTime(input)).toBe(expected);
		});

		it('retorna a string original para data inválida', () => {
			expect(fmtDateTime('xx-xx-xx')).toBe('xx-xx-xx');
		});
	});

	describe('toDateInputValue', () => {
		it('converte Date local para YYYY-MM-DD', () => {
			expect(toDateInputValue(new Date(2026, 0, 5))).toBe('2026-01-05');
		});

		it('usa a data atual por padrão e retorna formato válido', () => {
			const result = toDateInputValue();
			expect(/^\d{4}-\d{2}-\d{2}$/.test(result)).toBe(true);
		});
	});

	describe('daysFromToday', () => {
		it('0 quando a data é hoje', () => {
			const now = new Date(2026, 8, 21, 14, 30);
			expect(daysFromToday('2026-09-21', now)).toBe(0);
		});

		it('positivo para dias no futuro', () => {
			const now = new Date(2026, 8, 21, 0, 0);
			expect(daysFromToday('2026-09-25', now)).toBe(4);
		});

		it('negativo para dias no passado', () => {
			const now = new Date(2026, 8, 21, 23, 59);
			expect(daysFromToday('2026-09-18', now)).toBe(-3);
		});
	});
});
