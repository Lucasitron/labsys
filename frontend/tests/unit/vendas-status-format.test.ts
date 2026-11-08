import { describe, expect, it } from 'vitest';
import {
	codigoPrefix,
	formatDateBR,
	formatMoneyBRL
} from '$lib/utils/vendas-format';
import {
	interacaoTipoMeta,
	kanbanStatusMeta,
	orcamentoStatusMeta,
	plataformaMeta,
	prioridadeMeta,
	tarefaStatusMeta
} from '$lib/utils/vendas-status';

describe('vendas-status — orcamentoStatusMeta cobre a union de vnd-001', () => {
	it('Pendente → warn', () => {
		expect(orcamentoStatusMeta('Pendente')).toEqual({ label: 'Pendente', color: 'warn' });
	});

	it('Aprovado → success', () => {
		expect(orcamentoStatusMeta('Aprovado')).toEqual({ label: 'Aprovado', color: 'success' });
	});

	it('Recusado → danger', () => {
		expect(orcamentoStatusMeta('Recusado')).toEqual({ label: 'Recusado', color: 'danger' });
	});

	it('Ajuste → brand', () => {
		expect(orcamentoStatusMeta('Ajuste')).toEqual({ label: 'Ajuste', color: 'brand' });
	});
});

describe('vendas-status — kanbanStatusMeta cobre as 5 colunas', () => {
	it.each([
		['Fila', 'muted'],
		['Produção', 'brand'],
		['Acabamento', 'warn'],
		['Pronto', 'success'],
		['Entregue', 'muted']
	] as const)('%s → %s', (status, color) => {
		expect(kanbanStatusMeta(status)).toEqual({ label: status, color });
	});
});

describe('vendas-status — plataforma/tarefa/prioridade/interação', () => {
	it('plataformaMeta: ML → warn, Shopee → success, Elo7 → brand', () => {
		expect(plataformaMeta('Mercado Livre')).toEqual({ label: 'Mercado Livre', color: 'warn' });
		expect(plataformaMeta('Shopee')).toEqual({ label: 'Shopee', color: 'success' });
		expect(plataformaMeta('Elo7')).toEqual({ label: 'Elo7', color: 'brand' });
	});

	it('tarefaStatusMeta: Pendente → warn, Em Andamento → brand, Concluída → success', () => {
		expect(tarefaStatusMeta('Pendente')).toEqual({ label: 'Pendente', color: 'warn' });
		expect(tarefaStatusMeta('Em Andamento')).toEqual({
			label: 'Em Andamento',
			color: 'brand'
		});
		expect(tarefaStatusMeta('Concluída')).toEqual({ label: 'Concluída', color: 'success' });
	});

	it('prioridadeMeta: Alta → danger, Média → warn, Baixa → muted', () => {
		expect(prioridadeMeta('Alta')).toEqual({ label: 'Alta', color: 'danger' });
		expect(prioridadeMeta('Média')).toEqual({ label: 'Média', color: 'warn' });
		expect(prioridadeMeta('Baixa')).toEqual({ label: 'Baixa', color: 'muted' });
	});

	it('interacaoTipoMeta: E-mail → brand, Telefone → warn, Reunião/WhatsApp → success', () => {
		expect(interacaoTipoMeta('E-mail')).toEqual({ label: 'E-mail', color: 'brand' });
		expect(interacaoTipoMeta('Telefone')).toEqual({ label: 'Telefone', color: 'warn' });
		expect(interacaoTipoMeta('Reunião')).toEqual({ label: 'Reunião', color: 'success' });
		expect(interacaoTipoMeta('WhatsApp')).toEqual({ label: 'WhatsApp', color: 'success' });
	});
});

describe('vendas-format — moeda/data/códigos pt-BR', () => {
	it('formatMoneyBRL formata R$ 1.240,00', () => {
		const saida = formatMoneyBRL(1240);
		expect(saida.startsWith('R$')).toBe(true);
		expect(saida).toContain('1.240,00');
	});

	it('formatMoneyBRL formata centavos (99,90)', () => {
		expect(formatMoneyBRL(99.9)).toContain('99,90');
	});

	it('formatDateBR formata 18/09/2026', () => {
		expect(formatDateBR('2026-09-18T15:00:00.000Z')).toBe('18/09/2026');
	});

	it('codigoPrefix usa CLI-/OC-/EN- com 4 dígitos', () => {
		expect(codigoPrefix('CLI', 7)).toBe('CLI-0007');
		expect(codigoPrefix('OC', '12')).toBe('OC-0012');
		expect(codigoPrefix('EN', 123)).toBe('EN-0123');
	});
});
