import { describe, expect, it } from 'vitest';
import { arrParam, boolParam, intParam, sanitizeSort, strParam } from '$lib/utils/stock-url';

describe('estoque/stock-url', () => {
	describe('strParam', () => {
		it('retorna o valor com trim', () => {
			expect(strParam(new URLSearchParams('q=  parafuso  '), 'q')).toBe('parafuso');
		});

		it('retorna string vazia quando ausente', () => {
			expect(strParam(new URLSearchParams(), 'q')).toBe('');
		});
	});

	describe('intParam', () => {
		it('retorna inteiro válido dentro dos limites', () => {
			expect(intParam(new URLSearchParams('page=5'), 'page', 1, 1, 100)).toBe(5);
		});

		it('clampa acima do máximo', () => {
			expect(intParam(new URLSearchParams('page=999'), 'page', 1, 1, 100)).toBe(100);
		});

		it('clampa abaixo do mínimo', () => {
			expect(intParam(new URLSearchParams('page=-3'), 'page', 1, 1, 100)).toBe(1);
		});

		it('usa fallback para não-inteiro', () => {
			expect(intParam(new URLSearchParams('page=2.5'), 'page', 7, 1, 100)).toBe(7);
			expect(intParam(new URLSearchParams('page=abc'), 'page', 7, 1, 100)).toBe(7);
		});

		it('ausente cai no mínimo (Number(null)=0 → clamp)', () => {
			expect(intParam(new URLSearchParams(), 'page', 7, 1, 100)).toBe(1);
		});
	});

	describe('arrParam', () => {
		it('coleta todos os valores e descarta strings vazias', () => {
			const params = new URLSearchParams('c=INSUMO&c=&c=FERRAMENTA&c=');
			expect(arrParam(params, 'c')).toEqual(['INSUMO', 'FERRAMENTA']);
		});

		it('retorna array vazio quando ausente', () => {
			expect(arrParam(new URLSearchParams(), 'c')).toEqual([]);
		});
	});

	describe('boolParam', () => {
		it('aceita "1" e "true" como verdadeiro', () => {
			expect(boolParam(new URLSearchParams('baixo=1'), 'baixo')).toBe(true);
			expect(boolParam(new URLSearchParams('baixo=true'), 'baixo')).toBe(true);
		});

		it('qualquer outro valor (inclusive ausente) é falso', () => {
			expect(boolParam(new URLSearchParams('baixo=0'), 'baixo')).toBe(false);
			expect(boolParam(new URLSearchParams('baixo=false'), 'baixo')).toBe(false);
			expect(boolParam(new URLSearchParams('baixo=yes'), 'baixo')).toBe(false);
			expect(boolParam(new URLSearchParams(), 'baixo')).toBe(false);
		});
	});

	describe('sanitizeSort', () => {
		const allowed = ['nome', 'quantidadeAtual', '-createdAt'];

		it('mantém valor permitido', () => {
			expect(sanitizeSort('nome', allowed)).toBe('nome');
			expect(sanitizeSort('-createdAt', allowed)).toBe('-createdAt');
		});

		it('retorna vazio para coluna desconhecida', () => {
			expect(sanitizeSort('estoqueMinimo', allowed)).toBe('');
			expect(sanitizeSort('DROP TABLE', allowed)).toBe('');
		});
	});
});