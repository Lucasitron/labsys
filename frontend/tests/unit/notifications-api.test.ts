import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { writable } from 'svelte/store';
import type {
	Notification,
	NotificationPreferences,
	PagedNotifications
} from '$lib/types/notifications';

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }));

const authMock = writable<{ user: null; token: string | null; isAuthenticated: boolean }>({
	user: null,
	token: null,
	isAuthenticated: false
});

vi.mock('$lib/api/client', () => ({
	apiFetch: apiFetchMock
}));

vi.mock('$lib/stores/auth', () => ({
	auth: authMock,
	logout: vi.fn()
}));

vi.mock('$app/navigation', () => ({ goto: vi.fn() }));
vi.mock('$app/environment', () => ({ browser: true }));

function pagina(items: Notification[] = []): PagedNotifications<Notification> {
	return { items, total: items.length, page: 1, pageSize: 10, totalPages: 1 };
}

function notif(id = 'n1'): Notification {
	return {
		id,
		type: 'sistema',
		title: 'Manutenção agendada',
		body: 'A fila da impressora foi liberada.',
		read: false,
		createdAt: '2026-09-21T10:00:00.000Z'
	};
}

const PREFS: NotificationPreferences = {
	encomenda: { inapp: true, email: true, push: true },
	estoque: { inapp: true, email: true, push: true },
	financeiro: { inapp: true, email: true, push: true },
	producao: { inapp: true, email: true, push: true },
	pessoas: { inapp: true, email: true, push: true },
	sistema: { inapp: true, email: true, push: false }
};

async function freshApi() {
	vi.resetModules();
	return await import('$lib/api/notifications');
}

beforeEach(() => {
	apiFetchMock.mockReset();
	authMock.set({ user: null, token: null, isAuthenticated: false });
});

afterEach(() => {
	vi.unstubAllEnvs();
});

describe('api/notifications — montagem de rotas e query strings', () => {
	it('listNotifications monta GET /notifications com page/pageSize/type/read preenchidos', async () => {
		apiFetchMock.mockResolvedValue(pagina([notif()]));
		const { listNotifications } = await freshApi();

		const result = await listNotifications({ page: 2, pageSize: 10, type: 'estoque', read: false });

		expect(apiFetchMock).toHaveBeenCalledTimes(1);
		const [path, init] = apiFetchMock.mock.calls[0]!;
		expect(path).toBe('/notifications?page=2&pageSize=10&type=estoque&read=false');
		expect(init!.method).toBeUndefined();
		expect(result).toEqual(pagina([notif()]));
	});

	it('listNotifications omite type/read vazios (status all → omitido)', async () => {
		apiFetchMock.mockResolvedValue(pagina());
		const { listNotifications } = await freshApi();

		await listNotifications({ page: 1, pageSize: 10 });

		expect(apiFetchMock.mock.calls[0]![0]).toBe('/notifications?page=1&pageSize=10');
	});

	it('listNotifications com read=true (status read) serializa read=true', async () => {
		apiFetchMock.mockResolvedValue(pagina());
		const { listNotifications } = await freshApi();

		await listNotifications({ read: true });

		expect(apiFetchMock.mock.calls[0]![0]).toBe('/notifications?read=true');
	});

	it('listNotifications sem nenhum param chama path puro sem query', async () => {
		apiFetchMock.mockResolvedValue(pagina());
		const { listNotifications } = await freshApi();

		await listNotifications();

		expect(apiFetchMock.mock.calls[0]![0]).toBe('/notifications');
	});

	it('envia Authorization Bearer quando há token na store', async () => {
		authMock.set({ user: null, token: 'tk-1', isAuthenticated: true });
		apiFetchMock.mockResolvedValue(pagina());
		const { listNotifications } = await freshApi();

		await listNotifications();

		const [, init] = apiFetchMock.mock.calls[0]!;
		expect((init!.headers as Record<string, string>).Authorization).toBe('Bearer tk-1');
	});

	it('markRead chama PATCH /notifications/{id}/read', async () => {
		apiFetchMock.mockResolvedValue(undefined);
		const { markRead } = await freshApi();

		await markRead('n-7');

		const [path, init] = apiFetchMock.mock.calls[0]!;
		expect(path).toBe('/notifications/n-7/read');
		expect(init!.method).toBe('PATCH');
	});

	it('markAllRead chama POST /notifications/read-all', async () => {
		apiFetchMock.mockResolvedValue(undefined);
		const { markAllRead } = await freshApi();

		await markAllRead();

		const [path, init] = apiFetchMock.mock.calls[0]!;
		expect(path).toBe('/notifications/read-all');
		expect(init!.method).toBe('POST');
	});

	it('getPreferences chama GET /notifications/preferences e retorna a matriz', async () => {
		apiFetchMock.mockResolvedValue(PREFS);
		const { getPreferences } = await freshApi();

		const result = await getPreferences();

		expect(apiFetchMock.mock.calls[0]![0]).toBe('/notifications/preferences');
		expect(result).toEqual(PREFS);
	});

	it('savePreferences faz PUT /notifications/preferences com body JSON da matriz', async () => {
		apiFetchMock.mockResolvedValue(undefined);
		const { savePreferences } = await freshApi();

		await savePreferences(PREFS);

		const [path, init] = apiFetchMock.mock.calls[0]!;
		expect(path).toBe('/notifications/preferences');
		expect(init!.method).toBe('PUT');
		expect(init!.body).toBe(JSON.stringify(PREFS));
	});

	it('history monta query com search/type/read/channel/period quando presentes', async () => {
		apiFetchMock.mockResolvedValue({ items: [] });
		const { history } = await freshApi();

		await history({ search: 'pedido', type: 'encomenda', read: false, channel: 'email', period: '7d' });

		expect(apiFetchMock.mock.calls[0]![0]).toBe(
			'/notifications/history?search=pedido&type=encomenda&read=false&channel=email&period=7d'
		);
	});

	it('history omite filtros ausentes (chama path puro sem query)', async () => {
		apiFetchMock.mockResolvedValue({ items: [] });
		const { history } = await freshApi();

		await history();

		expect(apiFetchMock.mock.calls[0]![0]).toBe('/notifications/history');
	});

	it('re-exporta fetchUnreadCount de api/dashboard (sem duplicar)', async () => {
		const mod = await freshApi();
		expect(mod.fetchUnreadCount).toBeTypeOf('function');
	});
});