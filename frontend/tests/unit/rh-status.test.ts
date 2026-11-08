import { describe, expect, it } from 'vitest';
import {
	disponibilidadeMeta,
	guideStatusMeta,
	horaStatusMeta,
	nivelMeta,
	personStatusMeta,
	psStageMeta,
	trainingStatusMeta
} from '$lib/utils/rh-status';
import type { Meta } from '$lib/utils/stock-status';

type UnknownFn = (valor: never) => Meta;

function desconhecido(fn: UnknownFn): Meta {
	return fn('inexistente' as never);
}

describe('rh-status — personStatusMeta', () => {
	it('mapeia ativo/inativo/afastado com label+cor pt-BR', () => {
		expect(personStatusMeta('ativo')).toEqual({ label: 'Ativo', color: 'success' });
		expect(personStatusMeta('inativo')).toEqual({ label: 'Inativo', color: 'muted' });
		expect(personStatusMeta('afastado')).toEqual({ label: 'Afastado', color: 'warn' });
	});

	it('fallback desconhecido vira Desconhecido/muted', () => {
		expect(desconhecido(personStatusMeta)).toEqual({ label: 'Desconhecido', color: 'muted' });
	});
});

describe('rh-status — psStageMeta', () => {
	it('mapeia os 5 estágios do processo seletivo', () => {
		expect(psStageMeta('inscrito')).toEqual({ label: 'Inscrito', color: 'muted' });
		expect(psStageMeta('triagem')).toEqual({ label: 'Em triagem', color: 'brand' });
		expect(psStageMeta('entrevista')).toEqual({ label: 'Em entrevista', color: 'warn' });
		expect(psStageMeta('aprovado')).toEqual({ label: 'Aprovado', color: 'success' });
		expect(psStageMeta('reprovado')).toEqual({ label: 'Reprovado', color: 'danger' });
	});

	it('fallback desconhecido vira Desconhecido/muted', () => {
		expect(desconhecido(psStageMeta)).toEqual({ label: 'Desconhecido', color: 'muted' });
	});
});

describe('rh-status — horaStatusMeta', () => {
	it('mapeia pendente/validada/rejeitada', () => {
		expect(horaStatusMeta('pendente')).toEqual({ label: 'Pendente', color: 'warn' });
		expect(horaStatusMeta('validada')).toEqual({ label: 'Validada', color: 'success' });
		expect(horaStatusMeta('rejeitada')).toEqual({ label: 'Rejeitada', color: 'danger' });
	});

	it('fallback desconhecido vira Desconhecido/muted', () => {
		expect(desconhecido(horaStatusMeta)).toEqual({ label: 'Desconhecido', color: 'muted' });
	});
});

describe('rh-status — trainingStatusMeta', () => {
	it('mapeia os 6 status de treinamento (aberto+agendado/success)', () => {
		expect(trainingStatusMeta('aberto')).toEqual({ label: 'Inscrições abertas', color: 'success' });
		expect(trainingStatusMeta('agendado')).toEqual({ label: 'Agendado', color: 'success' });
		expect(trainingStatusMeta('solicitado')).toEqual({ label: 'Solicitado', color: 'brand' });
		expect(trainingStatusMeta('pendente')).toEqual({ label: 'Pendente', color: 'warn' });
		expect(trainingStatusMeta('em_andamento')).toEqual({ label: 'Em andamento', color: 'brand' });
		expect(trainingStatusMeta('concluido')).toEqual({ label: 'Concluído', color: 'muted' });
	});

	it('fallback desconhecido vira Desconhecido/muted', () => {
		expect(desconhecido(trainingStatusMeta)).toEqual({ label: 'Desconhecido', color: 'muted' });
	});
});

describe('rh-status — disponibilidadeMeta', () => {
	it('mapeia livre/ocupada/indisponivel', () => {
		expect(disponibilidadeMeta('livre')).toEqual({ label: 'Livre', color: 'success' });
		expect(disponibilidadeMeta('ocupada')).toEqual({ label: 'Ocupada', color: 'warn' });
		expect(disponibilidadeMeta('indisponivel')).toEqual({
			label: 'Indisponível',
			color: 'danger'
		});
	});

	it('fallback desconhecido vira Desconhecido/muted', () => {
		expect(desconhecido(disponibilidadeMeta)).toEqual({ label: 'Desconhecido', color: 'muted' });
	});
});

describe('rh-status — nivelMeta', () => {
	it('mapeia os 5 níveis (admin/danger, recrutando/muted)', () => {
		expect(nivelMeta('admin')).toEqual({ label: 'Admin', color: 'danger' });
		expect(nivelMeta('bolsista')).toEqual({ label: 'Bolsista', color: 'brand' });
		expect(nivelMeta('voluntario')).toEqual({ label: 'Voluntário', color: 'success' });
		expect(nivelMeta('estagiario')).toEqual({ label: 'Estagiário', color: 'warn' });
		expect(nivelMeta('recrutando')).toEqual({ label: 'Recrutando', color: 'muted' });
	});

	it('fallback desconhecido vira Desconhecido/muted', () => {
		expect(desconhecido(nivelMeta)).toEqual({ label: 'Desconhecido', color: 'muted' });
	});
});

describe('rh-status — guideStatusMeta', () => {
	it('mapeia validado/em_revisao/rascunho', () => {
		expect(guideStatusMeta('validado')).toEqual({ label: 'Validado', color: 'success' });
		expect(guideStatusMeta('em_revisao')).toEqual({ label: 'Em revisão', color: 'brand' });
		expect(guideStatusMeta('rascunho')).toEqual({ label: 'Rascunho', color: 'warn' });
	});

	it('fallback desconhecido vira Desconhecido/muted', () => {
		expect(desconhecido(guideStatusMeta)).toEqual({ label: 'Desconhecido', color: 'muted' });
	});
});
