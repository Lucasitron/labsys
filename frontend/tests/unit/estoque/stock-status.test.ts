import { describe, expect, it } from 'vitest';
import type { Emprestimo } from '$lib/types/stock';
import {
	ENTRY_KINDS,
	EXIT_REASONS,
	LOAN_TABS,
	availabilityTone,
	capacityTone,
	categoriaMeta,
	deriveItemStatus,
	entryKindMeta,
	exitReasonMeta,
	loanComputed,
	loanComputedMeta,
	loanConditionMeta,
	loanStatusMeta,
	localizacaoLabel,
	quantityTone,
	statusMeta
} from '$lib/utils/stock-status';

const TONES = ['success', 'warn', 'danger', 'brand', 'muted', 'ink'];

function isoDay(offsetDays: number): string {
	return new Date(Date.now() + offsetDays * 86_400_000).toISOString().slice(0, 10);
}

function emprestimo(overrides: Partial<Emprestimo> = {}): Emprestimo {
	return {
		id: 'l-1',
		idItem: 'it-1',
		nomeItem: 'Serra',
		idPessoa: 'p-1',
		quantidade: 1,
		dataEmprestimo: isoDay(-7),
		dataDevolucaoPrevista: isoDay(7),
		dataDevolucaoReal: null,
		status: 'ATIVO',
		loanComputed: 'no_prazo',
		...overrides
	};
}

describe('estoque/stock-status', () => {
	describe('statusMeta', () => {
		it('tem meta válida para os 5 status (label pt-BR + tone)', () => {
			const cases: [string, string, string][] = [
				['available', 'Disponível', 'success'],
				['low', 'Baixo', 'warn'],
				['out', 'Esgotado', 'danger'],
				['loaned', 'Emprestado', 'brand'],
				['maintenance', 'Manutenção', 'muted']
			];
			for (const [status, label, color] of cases) {
				const meta = statusMeta(status as Parameters<typeof statusMeta>[0]);
				expect(meta.label).toBe(label);
				expect(meta.color).toBe(color);
			}
			expect(TONES).toContain(statusMeta('available').color);
		});
	});

	describe('categoriaMeta', () => {
		it('mapeia o enum Categoria para labels pt-BR', () => {
			expect(categoriaMeta('INSUMO')).toEqual({ label: 'Insumo', color: 'brand' });
			expect(categoriaMeta('FERRAMENTA')).toEqual({ label: 'Ferramenta', color: 'success' });
			expect(categoriaMeta('PECA')).toEqual({ label: 'Peça', color: 'warn' });
		});
	});

	describe('entryKindMeta', () => {
		it('mapeia os 4 tipos de entrada', () => {
			expect(entryKindMeta('compra')).toEqual({ label: 'Compra', color: 'brand' });
			expect(entryKindMeta('doacao')).toEqual({ label: 'Doação', color: 'success' });
			expect(entryKindMeta('devolucao')).toEqual({ label: 'Devolução', color: 'warn' });
			expect(entryKindMeta('ajuste')).toEqual({ label: 'Ajuste', color: 'muted' });
		});

		it('constante ENTRY_KINDS cobre todos os tipos', () => {
			expect(ENTRY_KINDS).toEqual(['compra', 'doacao', 'devolucao', 'ajuste']);
		});
	});

	describe('exitReasonMeta (TipoSaida real)', () => {
		it('alinhado ao enum TipoSaida do backend', () => {
			expect(exitReasonMeta('CONSUMO')).toEqual({ label: 'Consumo interno', color: 'muted' });
			expect(exitReasonMeta('PERDA')).toEqual({ label: 'Perda', color: 'danger' });
			expect(exitReasonMeta('AJUSTE')).toEqual({ label: 'Ajuste', color: 'warn' });
			expect(exitReasonMeta('EMPRESTIMO')).toEqual({ label: 'Empréstimo', color: 'brand' });
		});

		it('constante EXIT_REASONS cobre todos os motivos', () => {
			expect(EXIT_REASONS).toEqual(['CONSUMO', 'PERDA', 'AJUSTE', 'EMPRESTIMO']);
		});
	});

	describe('loanComputedMeta / loanStatusMeta', () => {
		it('mapeia os 3 estados computados de prazo', () => {
			expect(loanComputedMeta('no_prazo')).toEqual({ label: 'No prazo', color: 'success' });
			expect(loanComputedMeta('vence_hoje')).toEqual({ label: 'Vence hoje', color: 'warn' });
			expect(loanComputedMeta('atrasado')).toEqual({ label: 'Atrasado', color: 'danger' });
		});

		it('status DEVOLVIDO renderiza muted (devolvido→muted)', () => {
			expect(loanStatusMeta('DEVOLVIDO')).toEqual({ label: 'Devolvido', color: 'muted' });
			expect(loanStatusMeta('ATIVO')).toEqual({ label: 'Ativo', color: 'brand' });
			expect(loanStatusMeta('ATRASADO')).toEqual({ label: 'Atrasado', color: 'danger' });
		});
	});

	describe('loanComputed (derivado client)', () => {
		const hoje = isoDay(0);

		it('devolvido nunca computa atraso — sempre no_prazo', () => {
			expect(
				loanComputed(emprestimo({ status: 'DEVOLVIDO', dataDevolucaoPrevista: isoDay(-30), dataDevolucaoReal: hoje }))
			).toBe('no_prazo');
		});

		it('vence_hoje quando dataDevolucaoPrevista é hoje', () => {
			expect(loanComputed(emprestimo({ status: 'ATIVO', dataDevolucaoPrevista: hoje }))).toBe('vence_hoje');
		});

		it('atrasado quando dataDevolucaoPrevista passou', () => {
			expect(loanComputed(emprestimo({ status: 'ATIVO', dataDevolucaoPrevista: isoDay(-1) }))).toBe('atrasado');
		});

		it('no_prazo quando dataDevolucaoPrevista ainda está por vir', () => {
			expect(loanComputed(emprestimo({ status: 'ATIVO', dataDevolucaoPrevista: isoDay(1) }))).toBe('no_prazo');
		});
	});

	describe('deriveItemStatus', () => {
		it('zero ou negativo = esgotado', () => {
			expect(deriveItemStatus(0, 5)).toBe('out');
			expect(deriveItemStatus(-2, 5)).toBe('out');
		});

		it('abaixo do mínimo = baixo', () => {
			expect(deriveItemStatus(3, 5)).toBe('low');
		});

		it('igual ou acima do mínimo = disponível', () => {
			expect(deriveItemStatus(5, 5)).toBe('available');
			expect(deriveItemStatus(10, 5)).toBe('available');
		});
	});

	describe('quantityTone', () => {
		it('estado corrente define o tom', () => {
			expect(quantityTone(0, 5)).toBe('danger');
			expect(quantityTone(-1, 5)).toBe('danger');
			expect(quantityTone(3, 5)).toBe('warn');
			expect(quantityTone(5, 5)).toBe('ink');
			expect(quantityTone(9, 5)).toBe('ink');
		});
	});

	describe('capacityTone / availabilityTone', () => {
		it('capacityTone: 90+ danger, 65-89 warn, resto success', () => {
			expect(capacityTone(90)).toBe('danger');
			expect(capacityTone(65)).toBe('warn');
			expect(capacityTone(64)).toBe('success');
			expect(capacityTone(0)).toBe('success');
		});

		it('availabilityTone: ok verde, faltam vermelho', () => {
			expect(availabilityTone('ok')).toBe('success');
			expect(availabilityTone('faltam')).toBe('danger');
		});
	});

	describe('localizacaoLabel', () => {
		it('sem localização retorna "Sem local"', () => {
			expect(localizacaoLabel(null)).toBe('Sem local');
		});

		it('monta A · P · C · descrição com as partes presentes', () => {
			expect(
				localizacaoLabel({ id: 'loc-1', armario: 'A1', prateleira: 'P2', caixa: 'C3', descricao: 'gaveta' })
			).toBe('A: A1 · P: P2 · C: C3 · gaveta');
		});

		it('omite partes opcionais ausentes', () => {
			expect(localizacaoLabel({ id: 'loc-2', armario: 'B7' })).toBe('A: B7');
		});
	});

	describe('loanConditionMeta', () => {
		it('mapeia condições de devolução', () => {
			expect(loanConditionMeta('bom')).toEqual({ label: 'Bom estado', color: 'success' });
			expect(loanConditionMeta('avaria')).toEqual({ label: 'Com avaria', color: 'warn' });
			expect(loanConditionMeta('danificado')).toEqual({ label: 'Danificado', color: 'danger' });
		});
	});

	describe('LOAN_TABS', () => {
		it('expõe as 3 tabs de empréstimos', () => {
			expect(LOAN_TABS).toEqual([
				{ key: 'ativos', label: 'Ativos' },
				{ key: 'atrasados', label: 'Atrasados' },
				{ key: 'historico', label: 'Histórico' }
			]);
		});
	});
});