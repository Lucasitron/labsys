import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { writable } from 'svelte/store';
import type { ApiError } from '$lib/api/client';
import type { DashboardSummary } from '$lib/types/dashboard';

const gotoMock = vi.fn();
const logoutMock = vi.fn();
const authMock = writable<{ user: null; token: string | null; isAuthenticated: boolean }>({
	user: null,
	token: null,
	isAuthenticated: false
});

vi.mock('$app/navigation', () => ({
	goto: gotoMock
}));

vi.mock('$lib/stores/auth', () => ({
	auth: authMock,
	logout: logoutMock
}));

vi.mock('$app/environment', () => ({
	browser: true
}));

const SUMMARY: DashboardSummary = {
	tasks: [],
	kpis: { ordersActive: { value: 3 }, notificationsUnread: { value: 1 } },
	ordersByStatus: [],
	machinesByStatus: [],
	activity: []
};

function jsonResponse(body: unknown, status = 200): Response {
	return new Response(JSON.stringify(body), {
		status,
		headers: { 'Content-Type': 'application/json' }
	});
}

async function freshDashboard() {
	vi.resetModules();
	return await import('$lib/api/dashboard');
}

beforeEach(() => {
	vi.stubEnv('VITE_API_BASE_URL', 'http://api.test');
	gotoMock.mockReset();
	logoutMock.mockReset();
	authMock.set({ user: null, token: null, isAuthenticated: false });
	window.history.replaceState({}, '', '/');
});

afterEach(() => {
	vi.unstubAllEnvs();
	window.history.replaceState({}, '', '/');
});

describe('api/dashboard', () => {
	it('fetchSummary monta GET /dashboard/summary com Content-Type JSON e retorna JSON tipado', async () => {
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse(SUMMARY));
		const { fetchSummary } = await freshDashboard();

		const result = await fetchSummary(fetchMock);

		expect(fetchMock).toHaveBeenCalledTimes(1);
		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/dashboard/summary');
		expect((init!.headers as Headers).get('Content-Type')).toBe('application/json');
		expect(result).toEqual(SUMMARY);
	});

	it('fetchUnreadCount monta GET /notifications/unread/count', async () => {
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ count: 2 }));
		const { fetchUnreadCount } = await freshDashboard();

		const result = await fetchUnreadCount(fetchMock);

		expect(fetchMock).toHaveBeenCalledTimes(1);
		expect(fetchMock.mock.calls[0]![0]).toBe('http://api.test/notifications/unread/count');
		expect(result).toEqual({ count: 2 });
	});

	it('completeTask envia PATCH /tasks/{id} com body {done:true}', async () => {
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ ok: true }));
		vi.stubGlobal('fetch', fetchMock);
		const { completeTask } = await freshDashboard();

		await completeTask('t-42');

		expect(fetchMock).toHaveBeenCalledTimes(1);
		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/tasks/t-42');
		expect(init!.method).toBe('PATCH');
		expect(init!.body).toBe(JSON.stringify({ done: true }));
		expect((init!.headers as Headers).get('Content-Type')).toBe('application/json');
	});

	it('envia Authorization Bearer quando há token na store', async () => {
		authMock.set({ user: null, token: 'tk-123', isAuthenticated: true });
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse(SUMMARY));
		const { fetchSummary } = await freshDashboard();

		await fetchSummary(fetchMock);

		const [, init] = fetchMock.mock.calls[0]!;
		expect((init!.headers as Headers).get('Authorization')).toBe('Bearer tk-123');
	});

	it('não envia Authorization quando não autenticado (sem "Bearer null")', async () => {
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse(SUMMARY));
		const { fetchSummary } = await freshDashboard();

		await fetchSummary(fetchMock);

		const [, init] = fetchMock.mock.calls[0]!;
		expect((init!.headers as Headers).get('Authorization')).toBeNull();
	});

	it('401 em rota protegida lança ApiError sem efeito local (interceptor global cobre)', async () => {
		window.history.pushState({}, '', '/auth/login');
		const fetchMock = vi
			.fn<typeof fetch>()
			.mockResolvedValue(jsonResponse({ error: 'EXPIRED', message: 'sessão expirada' }, 401));
		const { fetchSummary } = await freshDashboard();
		const { ApiError } = await import('$lib/api/client');

		const err = await fetchSummary(fetchMock).catch((e: unknown) => e);

		expect(err).toBeInstanceOf(ApiError);
		expect((err as ApiError).status).toBe(401);
		expect((err as ApiError).code).toBe('EXPIRED');
		expect(gotoMock).not.toHaveBeenCalled();
		expect(logoutMock).not.toHaveBeenCalled();
	});

	it('401 delega ao interceptor global: logout único e redirect sanitizado (sem destino hardcoded local)', async () => {
		window.history.pushState({}, '', '/dashboard');
		const fetchMock = vi
			.fn<typeof fetch>()
			.mockResolvedValue(jsonResponse({ error: 'EXPIRED', message: 'expirou' }, 401));
		const { fetchSummary } = await freshDashboard();
		const { ApiError } = await import('$lib/api/client');

		const err = await fetchSummary(fetchMock).catch((e: unknown) => e);

		expect(err).toBeInstanceOf(ApiError);
		expect((err as ApiError).status).toBe(401);
		expect(logoutMock).toHaveBeenCalledTimes(1);
		expect(gotoMock).toHaveBeenCalledTimes(1);
		expect(gotoMock).toHaveBeenCalledWith('/auth/login?redirect=%2Fdashboard');
	});

	it('5xx com payload {code,message} rethrows ApiError preservando status e code', async () => {
		const fetchMock = vi
			.fn<typeof fetch>()
			.mockResolvedValue(jsonResponse({ code: 'SERVER_ERROR', message: 'falhou' }, 500));
		const { fetchSummary } = await freshDashboard();
		const { ApiError } = await import('$lib/api/client');

		const err = await fetchSummary(fetchMock).catch((e: unknown) => e);

		expect(err).toBeInstanceOf(ApiError);
		expect((err as ApiError).status).toBe(500);
		expect((err as ApiError).code).toBe('SERVER_ERROR');
	});
});