import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { ApiError } from '$lib/api/client';

const gotoMock = vi.fn();
const logoutMock = vi.fn();

vi.mock('$app/navigation', () => ({
	goto: gotoMock
}));

vi.mock('$lib/stores/auth', () => ({
	logout: logoutMock
}));

vi.mock('$app/environment', () => ({
	browser: true
}));

function jsonResponse(body: unknown, status = 200): Response {
	return new Response(JSON.stringify(body), {
		status,
		headers: { 'Content-Type': 'application/json' }
	});
}

async function freshClient() {
	vi.resetModules();
	return await import('$lib/api/client');
}

beforeEach(() => {
	vi.stubEnv('VITE_API_BASE_URL', 'http://api.test');
	gotoMock.mockReset();
	logoutMock.mockReset();
});

afterEach(() => {
	vi.unstubAllEnvs();
	vi.useRealTimers();
	window.history.replaceState({}, '', '/');
});

describe('apiFetch', () => {
	it('monta URL com API_BASE e header Content-Type JSON', async () => {
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ id: 'i1' }));
		const { apiFetch } = await freshClient();

		await apiFetch('/estoque/itens', {}, fetchMock);

		expect(fetchMock).toHaveBeenCalledTimes(1);
		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/estoque/itens');
		expect(init).toBeDefined();
		expect(init!.headers).toBeInstanceOf(Headers);
		expect((init!.headers as Headers).get('Content-Type')).toBe('application/json');
		expect(init!.signal).toBeInstanceOf(AbortSignal);
	});

	it('sucesso retorna JSON tipado', async () => {
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ total: 3 }));
		const { apiFetch } = await freshClient();

		const data = await apiFetch<{ total: number }>('/estoque/itens', {}, fetchMock);

		expect(data).toEqual({ total: 3 });
	});

	it('timeout de 15s aborta e lança NetworkError', async () => {
		vi.useFakeTimers();
		const fetchMock = vi
			.fn<typeof fetch>()
			.mockImplementation(
				(_url, init) =>
					new Promise<Response>((_resolve, reject) => {
						init?.signal?.addEventListener('abort', () => {
							reject(new DOMException('The operation was aborted', 'AbortError'));
						});
					})
			);
		const { apiFetch, NetworkError } = await freshClient();

		const promise = apiFetch('/lento', {}, fetchMock);
		vi.advanceTimersByTime(15_000);

		const err = await promise.catch((e: unknown) => e);
		expect(err).toBeInstanceOf(NetworkError);
		expect((err as Error).message).toBe('Tempo limite excedido. Verifique sua rede.');
	});

	it('falha de rede lança NetworkError', async () => {
		const fetchMock = vi.fn<typeof fetch>().mockRejectedValue(new TypeError('Failed to fetch'));
		const { apiFetch, NetworkError } = await freshClient();

		const err = await apiFetch('/x', {}, fetchMock).catch((e: unknown) => e);

		expect(err).toBeInstanceOf(NetworkError);
	});

	it('4xx com payload {code,message} lança ApiError com status e code', async () => {
		const fetchMock = vi
			.fn<typeof fetch>()
			.mockResolvedValue(jsonResponse({ code: 'ACCOUNT_LOCKED', message: 'bloqueada' }, 423));
		const { apiFetch, ApiError } = await freshClient();

		const err = await apiFetch('/auth/login', {}, fetchMock).catch((e: unknown) => e);

		expect(err).toBeInstanceOf(ApiError);
		expect((err as ApiError).status).toBe(423);
		expect((err as ApiError).code).toBe('ACCOUNT_LOCKED');
		expect((err as ApiError).message).toBe('bloqueada');
	});

	it('5xx usa campo error do payload como code', async () => {
		const fetchMock = vi
			.fn<typeof fetch>()
			.mockResolvedValue(jsonResponse({ error: 'SERVER_ERROR', message: 'falhou' }, 500));
		const { apiFetch, ApiError } = await freshClient();

		const err = await apiFetch('/auth/login', {}, fetchMock).catch((e: unknown) => e);

		expect(err).toBeInstanceOf(ApiError);
		expect((err as ApiError).status).toBe(500);
		expect((err as ApiError).code).toBe('SERVER_ERROR');
	});

	it('401 em rota protegida chama logout, redireciona com path sanitizado e rethrow do ApiError', async () => {
		window.history.pushState({}, '', '/estoque');
		const fetchMock = vi
			.fn<typeof fetch>()
			.mockResolvedValue(jsonResponse({ error: 'EXPIRED', message: 'sessao expirada' }, 401));
		const { apiFetch, ApiError } = await freshClient();

		const err = await apiFetch('/estoque/itens', {}, fetchMock).catch((e: unknown) => e);

		expect(err).toBeInstanceOf(ApiError);
		expect((err as ApiError).status).toBe(401);
		expect(logoutMock).toHaveBeenCalledTimes(1);
		expect(gotoMock).toHaveBeenCalledWith('/auth/login?redirect=%2Festoque');
	});

	it('401 em /auth/login não chama logout nem redireciona (opt-out)', async () => {
		window.history.pushState({}, '', '/estoque');
		const fetchMock = vi
			.fn<typeof fetch>()
			.mockImplementation(() => Promise.resolve(jsonResponse({ error: 'INVALID_CREDENTIALS' }, 401)));
		const { apiFetch, ApiError } = await freshClient();

		const err = await apiFetch('/auth/login', { method: 'POST' }, fetchMock).catch((e: unknown) => e);

		expect(err).toBeInstanceOf(ApiError);
		expect((err as ApiError).status).toBe(401);
		expect((err as ApiError).code).toBe('INVALID_CREDENTIALS');
		expect(logoutMock).not.toHaveBeenCalled();
		expect(gotoMock).not.toHaveBeenCalled();
	});

	it('flag anti-duplicidade evita logout e navegação duplicados em 401 concorrentes', async () => {
		window.history.pushState({}, '', '/estoque');
		const fetchMock = vi
			.fn<typeof fetch>()
			.mockImplementation(() => Promise.resolve(jsonResponse({ message: 'expirou' }, 401)));
		const { apiFetch } = await freshClient();

		await Promise.allSettled([
			apiFetch('/estoque/itens', {}, fetchMock),
			apiFetch('/estoque/itens', {}, fetchMock)
		]);

		expect(logoutMock).toHaveBeenCalledTimes(1);
		expect(gotoMock).toHaveBeenCalledTimes(1);
	});

	it('redirect sempre sanitizado: paths externos caem em /dashboard', async () => {
		const { sanitizeRedirect } = await freshClient();

		for (const raw of ['//evil.com', 'https://evil.com', 'javascript:alert(1)', '/a\\b', '/a:b']) {
			expect(sanitizeRedirect(raw)).toBe('/dashboard');
		}
	});

	it('interceptor sanitiza path atual antes de montar o redirect', async () => {
		window.history.pushState({}, '', '/estoque:1');
		const fetchMock = vi
			.fn<typeof fetch>()
			.mockResolvedValue(jsonResponse({ message: 'expirou' }, 401));
		const { apiFetch } = await freshClient();

		await apiFetch('/estoque/itens', {}, fetchMock).catch(() => undefined);

		expect(gotoMock).toHaveBeenCalledWith('/auth/login?redirect=%2Fdashboard');
	});

	it('só aceita path interno no sanitizador', async () => {
		const { sanitizeRedirect } = await freshClient();

		expect(sanitizeRedirect(null)).toBe('/dashboard');
		expect(sanitizeRedirect('')).toBe('/dashboard');
		expect(sanitizeRedirect('/estoque')).toBe('/estoque');
		expect(sanitizeRedirect('/estoque?filtro=1')).toBe('/estoque?filtro=1');
		expect(sanitizeRedirect('//double')).toBe('/dashboard');
		expect(sanitizeRedirect('/a\\b')).toBe('/dashboard');
		expect(sanitizeRedirect('/a:b')).toBe('/dashboard');
		expect(sanitizeRedirect('/a\u0000b')).toBe('/dashboard');
	});
});