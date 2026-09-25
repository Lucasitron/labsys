import { describe, expect, it } from 'vitest';
import {
	compraStatusMeta,
	doacaoTipoMeta,
	fechamentoStatusMeta,
	lancamentoStatusMeta,
	tipoMeta
} from '$lib/utils/financeiro-status';
import { codigoLancamento, formatSignedBRL } from '$lib/utils/financeiro-format';
import { formatMoneyBRL } from '$lib/utils/vendas-format';

describe('financeiro-status — lancamentoStatusMeta cobre a union de fin-001', () => {
	it.each([
		['Pendente', 'warn'],
		['Pago', 'success'],
		['Atrasado', 'danger'],
		['Cancelado', 'muted']
	] as const)('%s → %s', (status, color) => {
		expect(lancamentoStatusMeta(status)).toEqual({ label: status, color });
	});
});

describe('financeiro-status — tipoMeta cobre Entrada/Saída', () => {
	it('Entrada → success', () => {
		expect(tipoMeta('Entrada')).toEqual({ label: 'Entrada', color: 'success' });
	});

	it('Saída → danger', () => {
		expect(tipoMeta('Saída')).toEqual({ label: 'Saída', color: 'danger' });
	});
});

describe('financeiro-status — fechamentoStatusMeta cobre a union de fin-001', () => {
	it.each([
		['Aberta', 'brand'],
		['Concluída', 'success'],
		['Cancelada', 'muted']
	] as const)('%s → %s', (status, color) => {
		expect(fechamentoStatusMeta(status)).toEqual({ label: status, color });
	});
});

describe('financeiro-status — compraStatusMeta cobre a union de fin-001', () => {
	it.each([
		['Registrada', 'brand'],
		['Visualizada', 'warn'],
		['Concluída', 'success']
	] as const)('%s → %s', (status, color) => {
		expect(compraStatusMeta(status)).toEqual({ label: status, color });
	});
});

describe('financeiro-status — doacaoTipoMeta cobre Doação/Projeto', () => {
	it('Doação → brand', () => {
		expect(doacaoTipoMeta('Doação')).toEqual({ label: 'Doação', color: 'brand' });
	});

	it('Projeto → success', () => {
		expect(doacaoTipoMeta('Projeto')).toEqual({ label: 'Projeto', color: 'success' });
	});
});

describe('financeiro-format — formatSignedBRL com sinal +/− pt-BR', () => {
	it('Entrada prefixa + e mantém R$ 1.240,00', () => {
		const saida = formatSignedBRL(1240, 'Entrada');

		expect(saida).toBe(`+${formatMoneyBRL(1240)}`);
		expect(saida.startsWith('+')).toBe(true);
		expect(saida).toContain('1.240,00');
	});

	it('Saída prefixa − (U+2212) e mantém R$ 380,00', () => {
		const saida = formatSignedBRL(380, 'Saída');

		expect(saida).toBe(`−${formatMoneyBRL(380)}`);
		expect(saida[0]).toBe('−');
		expect(saida).toContain('380,00');
	});
});

describe('financeiro-format — codigoLancamento usa prefixo LAN-', () => {
	it('número usa LAN- com 4 dígitos', () => {
		expect(codigoLancamento(7)).toBe('LAN-0007');
		expect(codigoLancamento(123)).toBe('LAN-0123');
	});

	it('string preserva prefixo LAN-', () => {
		expect(codigoLancamento('42')).toBe('LAN-0042');
		expect(codigoLancamento('0007')).toBe('LAN-0007');
	});
});
