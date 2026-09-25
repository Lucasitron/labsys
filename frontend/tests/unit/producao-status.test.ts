import { describe, expect, it } from 'vitest';
import {
	CARTAO_META,
	CHAMADO_STATUS_META,
	GRAVIDADE_META,
	INSPECAO_STATUS_META,
	KANBAN_COLUNA_META,
	MAQUINA_STATUS_META,
	MESA_AUDITORIA_META,
	MESA_STATUS_META,
	PENALIDADE_STATUS_META,
	PENALIDADE_TIPO_META,
	PENDENCIA_STATUS_META,
	PREVENTIVA_STATUS_META,
	PROJETO_STATUS_META,
	TAREFA_STATUS_META,
	gravidadeMeta,
	notaToCartao,
	slugTestId,
	type Meta
} from '$lib/utils/producao-status';

const TONES = ['success', 'warn', 'danger', 'brand', 'muted', 'ink'];

function expectMetaMap(map: Record<string, Meta>, expectedKeys: string[]) {
	const keys = Object.keys(map);
	expect(keys).toHaveLength(expectedKeys.length);
	expect(keys).toEqual(expect.arrayContaining(expectedKeys));
	for (const key of keys) {
		const meta = map[key];
		expect(typeof meta.label).toBe('string');
		expect(meta.label.length).toBeGreaterThan(0);
		expect(TONES).toContain(meta.color);
	}
}

describe('producao-status', () => {
	describe('notaToCartao', () => {
		it('retorna Verde para notas >= 90', () => {
			expect(notaToCartao(90)).toBe('Verde');
			expect(notaToCartao(95)).toBe('Verde');
			expect(notaToCartao(100)).toBe('Verde');
		});

		it('retorna Vermelho para notas < 70', () => {
			expect(notaToCartao(0)).toBe('Vermelho');
			expect(notaToCartao(50)).toBe('Vermelho');
			expect(notaToCartao(69)).toBe('Vermelho');
		});

		it('retorna Amarelo para notas entre 70 e 89 (cobre as bordas)', () => {
			expect(notaToCartao(70)).toBe('Amarelo');
			expect(notaToCartao(80)).toBe('Amarelo');
			expect(notaToCartao(89)).toBe('Amarelo');
		});

		it('nota decimal entre faixa também é Amarelo', () => {
			expect(notaToCartao(79.5)).toBe('Amarelo');
		});
	});

	describe('maps *_META', () => {
		it('PROJETO_STATUS_META tem chaves e metadados válidos', () => {
			expectMetaMap(PROJETO_STATUS_META, ['Planejado', 'Em andamento', 'Concluído', 'Cancelado']);
		});

		it('TAREFA_STATUS_META tem chaves e metadados válidos', () => {
			expectMetaMap(TAREFA_STATUS_META, [
				'Pendente',
				'Em andamento',
				'Concluída',
				'Atrasada',
				'Bloqueada'
			]);
		});

		it('KANBAN_COLUNA_META tem chaves e metadados válidos', () => {
			expectMetaMap(KANBAN_COLUNA_META, [
				'Fila',
				'Produção',
				'Acabamento',
				'Pronto',
				'Entregue'
			]);
		});

		it('MAQUINA_STATUS_META tem chaves e metadados válidos', () => {
			expectMetaMap(MAQUINA_STATUS_META, ['Disponível', 'Em manutenção', 'Suspensa']);
		});

		it('CHAMADO_STATUS_META tem chaves e metadados válidos', () => {
			expectMetaMap(CHAMADO_STATUS_META, [
				'Aberto',
				'Atribuído',
				'Em andamento',
				'Aguardando compra',
				'Resolvido'
			]);
		});

		it('PREVENTIVA_STATUS_META tem chaves e metadados válidos', () => {
			expectMetaMap(PREVENTIVA_STATUS_META, ['Programada', 'Concluída', 'Atrasada']);
		});

		it('INSPECAO_STATUS_META tem chaves e metadados válidos', () => {
			expectMetaMap(INSPECAO_STATUS_META, ['Programada', 'Concluída', 'Atrasada']);
		});

		it('CARTAO_META tem chaves e metadados válidos', () => {
			expectMetaMap(CARTAO_META, ['Verde', 'Amarelo', 'Vermelho']);
		});

		it('GRAVIDADE_META tem chaves e metadados válidos', () => {
			expectMetaMap(GRAVIDADE_META, ['Leve', 'Moderada', 'Grave']);
		});

		it('PENDENCIA_STATUS_META tem chaves e metadados válidos', () => {
			expectMetaMap(PENDENCIA_STATUS_META, ['Aberta', 'Resolvida']);
		});

		it('PENALIDADE_TIPO_META tem chaves e metadados válidos', () => {
			expectMetaMap(PENALIDADE_TIPO_META, ['Aviso', 'Advertência', 'Suspensão', 'Expulsão']);
		});

		it('PENALIDADE_STATUS_META tem chaves e metadados válidos', () => {
			expectMetaMap(PENALIDADE_STATUS_META, ['Ativa', 'Cumprida']);
		});

		it('MESA_STATUS_META tem chaves e metadados válidos', () => {
			expectMetaMap(MESA_STATUS_META, ['Aprovada', 'Pendente']);
		});

		it('MESA_AUDITORIA_META tem chaves e metadados válidos', () => {
			expectMetaMap(MESA_AUDITORIA_META, ['ATIVO', 'ABANDONADO']);
		});

		it('labels são strings pt-BR legíveis (check dos cartões)', () => {
			expect(CARTAO_META.Verde.label).toBe('Verde');
			expect(CARTAO_META.Verde.color).toBe('success');
			expect(CARTAO_META.Amarelo.label).toBe('Amarelo');
			expect(CARTAO_META.Amarelo.color).toBe('warn');
			expect(CARTAO_META.Vermelho.label).toBe('Vermelho');
			expect(CARTAO_META.Vermelho.color).toBe('danger');
		});
	});

	describe('gravidadeMeta', () => {
		it('retorna a meta da gravidade informada', () => {
			expect(gravidadeMeta('Leve')).toBe(GRAVIDADE_META.Leve);
			expect(gravidadeMeta('Moderada')).toBe(GRAVIDADE_META.Moderada);
			expect(gravidadeMeta('Grave')).toBe(GRAVIDADE_META.Grave);
		});
	});

	describe('slugTestId', () => {
		it('slugifica acentos e espaços', () => {
			expect(slugTestId('Em andamento')).toBe('em-andamento');
			expect(slugTestId('Aguardando compra')).toBe('aguardando-compra');
			expect(slugTestId('Concluída')).toBe('concluida');
			expect(slugTestId('Setor 01 – CNC')).toBe('setor-01-cnc');
		});

		it('mantém identificadores já slugificados', () => {
			expect(slugTestId('maq-01')).toBe('maq-01');
			expect(slugTestId('admin')).toBe('admin');
		});
	});
});