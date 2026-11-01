import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('$app/navigation', () => ({
	goto: vi.fn()
}));

vi.mock('$lib/stores/auth', () => ({
	logout: vi.fn(),
	applyRefreshedTokens: vi.fn()
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
	window.localStorage.clear();
});

afterEach(() => {
	vi.unstubAllEnvs();
	window.localStorage.clear();
});

describe('client — auth automática', () => {
	it('injeta Authorization: Bearer <token> lendo localStorage[fablab.auth]', async () => {
		window.localStorage.setItem('fablab.auth', JSON.stringify({ token: 'tk-persisted' }));
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ ok: true }));
		const { apiFetch } = await freshClient();

		await apiFetch('/rh/pessoas', {}, fetchMock);

		const [, init] = fetchMock.mock.calls[0]!;
		expect((init!.headers as Headers).get('Authorization')).toBe('Bearer tk-persisted');
	});

	it('não injeta Authorization em rota pública /auth/login', async () => {
		window.localStorage.setItem('fablab.auth', JSON.stringify({ token: 'tk-persisted' }));
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ ok: true }));
		const { apiFetch } = await freshClient();

		await apiFetch('/auth/login', { method: 'POST' }, fetchMock);

		const [, init] = fetchMock.mock.calls[0]!;
		expect((init!.headers as Headers).get('Authorization')).toBeNull();
	});

	it('header Authorization explícito no init prevalece sobre o token persistido', async () => {
		window.localStorage.setItem('fablab.auth', JSON.stringify({ token: 'tk-persisted' }));
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ ok: true }));
		const { apiFetch } = await freshClient();

		await apiFetch('/rh/pessoas', { headers: { Authorization: 'Bearer tk-explicit' } }, fetchMock);

		const [, init] = fetchMock.mock.calls[0]!;
		expect((init!.headers as Headers).get('Authorization')).toBe('Bearer tk-explicit');
	});
});

describe('client — buildQuery', () => {
	it('gera query string com array, ignora undefined e vazio', async () => {
		const { buildQuery } = await freshClient();

		const qs = buildQuery({
			search: 'abc',
			page: 2,
			empty: undefined,
			blank: '',
			tags: ['a', 'b', '']
		});

		expect(qs).toBe('?search=abc&page=2&tags=a&tags=b');
	});

	it('retorna string vazia quando não há parâmetros válidos', async () => {
		const { buildQuery } = await freshClient();

		expect(buildQuery({ a: undefined, b: '' })).toBe('');
	});
});
