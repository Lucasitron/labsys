import { describe, expect, it } from 'vitest';
import {
	CHANNEL_BADGE,
	NOTIFICATION_TYPES,
	TYPE_META
} from '$lib/utils/notification-format';

describe('notification-format — identidade visual por tipo e canal', () => {
	it('TYPE_META cobre os 6 tipos com label/ícone/tone esperados', () => {
		expect(TYPE_META).toEqual({
			encomenda: { label: 'Encomenda', icon: 'shopping-bag', tone: 'brand' },
			estoque: { label: 'Estoque', icon: 'cube', tone: 'warn' },
			financeiro: { label: 'Financeiro', icon: 'chart', tone: 'success' },
			producao: { label: 'Produção', icon: 'wrench', tone: 'ink' },
			pessoas: { label: 'Pessoas & RH', icon: 'users', tone: 'brand' },
			sistema: { label: 'Sistema', icon: 'bell', tone: 'muted' }
		});
	});

	it('TYPE_META tem exatamente as mesmas chaves de NOTIFICATION_TYPES', () => {
		expect(Object.keys(TYPE_META).sort()).toEqual([...NOTIFICATION_TYPES].sort());
		expect(Object.keys(TYPE_META)).toHaveLength(6);
	});

	it('CHANNEL_BADGE cobre os 3 canais com label e tone', () => {
		expect(CHANNEL_BADGE).toEqual({
			inapp: { label: 'In-app', tone: 'brand' },
			email: { label: 'E-mail', tone: 'success' },
			push: { label: 'Push', tone: 'warn' }
		});
	});

	it('NOTIFICATION_TYPES segue a ordem de exibição do mockup', () => {
		expect(NOTIFICATION_TYPES).toEqual([
			'encomenda',
			'estoque',
			'financeiro',
			'producao',
			'pessoas',
			'sistema'
		]);
	});
});