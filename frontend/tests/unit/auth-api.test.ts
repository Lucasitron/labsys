import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { writable } from 'svelte/store';
import type { AuthState } from '$lib/stores/auth';
import type { MeResponse } from '$lib/types/auth';

const gotoMock = vi.fn();
const storeLogoutMock = vi.fn();
const authMock = writable<AuthState>({
	user: null,
	token: null,
	refreshToken: null,
	isAuthenticated: false
});

vi.mock('$app/navigation', () => ({
	goto: gotoMock
}));

vi.mock('$lib/stores/auth', () => ({
	auth: authMock,
	logout: storeLogoutMock
}));

vi.mock('$app/environment', () => ({
	browser: true
}));

const ME: MeResponse = {
	id: 99,
	idUser: 7,
	email: 'joao@fablab.org',
	nomeUsuario: 'joao',
	setor: null,
	permissions: [
		{ role: 0, label: 'Admin', active: false },
		{ role: 2, label: 'Voluntário', active: true }
	]
};

function jsonResponse(body: unknown, status = 200): Response {
	return new Response(JSON.stringify(body), {
		status,
		headers: { 'Content-Type': 'application/json' }
	});
}

async function freshApiAuth() {
	vi.resetModules();
	return await import('$lib/api/auth');
}

beforeEach(() => {
	vi.stubEnv('VITE_API_BASE_URL', 'http://api.test');
	gotoMock.mockReset();
	storeLogoutMock.mockReset();
	authMock.set({ user: null, token: null, refreshToken: null, isAuthenticated: false });
	window.history.replaceState({}, '', '/');
});

afterEach(() => {
	vi.unstubAllEnvs();
	vi.unstubAllGlobals();
	window.history.replaceState({}, '', '/');
});

describe('api/auth (contrato Laravel)', () => {
	it('login com nome de usuário envia {nomeUsuario, senha}', async () => {
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ ok: true }));
		vi.stubGlobal('fetch', fetchMock);
		const { login } = await freshApiAuth();

		await login('joao', 'segredo123');

		expect(fetchMock).toHaveBeenCalledTimes(1);
		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/auth/login');
		expect(init!.method).toBe('POST');
		expect(init!.body).toBe(JSON.stringify({ nomeUsuario: 'joao', senha: 'segredo123' }));
	});

	it('login com e-mail (contém @) envia {email, senha}', async () => {
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ ok: true }));
		vi.stubGlobal('fetch', fetchMock);
		const { login } = await freshApiAuth();

		await login('joao@fablab.org', 'segredo123');

		const [, init] = fetchMock.mock.calls[0]!;
		expect(init!.body).toBe(JSON.stringify({ email: 'joao@fablab.org', senha: 'segredo123' }));
	});

	it('refresh envia POST /auth/refresh com {refreshToken}', async () => {
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
			jsonResponse({ accessToken: 'a2', refreshToken: 'r2', tokenType: 'Bearer', expiresIn: 3600 })
		);
		vi.stubGlobal('fetch', fetchMock);
		const { refresh } = await freshApiAuth();

		const result = await refresh('refresh-1');

		expect(fetchMock).toHaveBeenCalledTimes(1);
		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/auth/refresh');
		expect(init!.method).toBe('POST');
		expect(init!.body).toBe(JSON.stringify({ refreshToken: 'refresh-1' }));
		expect(result.accessToken).toBe('a2');
		expect(result.refreshToken).toBe('r2');
	});

	it('logout envia POST /auth/logout com Bearer e token no body', async () => {
		authMock.set({ user: null, token: 'tk-1', refreshToken: 'rt-1', isAuthenticated: true });
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ message: 'ok' }));
		vi.stubGlobal('fetch', fetchMock);
		const { logout } = await freshApiAuth();

		await logout();

		expect(fetchMock).toHaveBeenCalledTimes(1);
		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/auth/logout');
		expect(init!.method).toBe('POST');
		expect((init!.headers as Headers).get('Authorization')).toBe('Bearer tk-1');
		expect(init!.body).toBe(JSON.stringify({ token: 'tk-1' }));
	});

	it('logout sem token não envia Authorization nem body', async () => {
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ message: 'ok' }));
		vi.stubGlobal('fetch', fetchMock);
		const { logout } = await freshApiAuth();

		await logout();

		const [, init] = fetchMock.mock.calls[0]!;
		expect((init!.headers as Headers).get('Authorization')).toBeNull();
		expect(init!.body).toBeUndefined();
	});

	it('fetchMeRaw busca GET /auth/me com Bearer explícito', async () => {
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse(ME));
		const { fetchMeRaw } = await freshApiAuth();

		const me = await fetchMeRaw('novo-token', fetchMock);

		expect(fetchMock).toHaveBeenCalledTimes(1);
		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/auth/me');
		expect((init!.headers as Headers).get('Authorization')).toBe('Bearer novo-token');
		expect(me).toEqual(ME);
	});

	it('fetchMe monta User a partir da permission ativa', async () => {
		authMock.set({ user: null, token: 'tk-1', refreshToken: null, isAuthenticated: false });
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse(ME));
		const { fetchMe } = await freshApiAuth();

		const user = await fetchMe(fetchMock);

		expect(user).toEqual({
			id: '7',
			username: 'joao',
			name: 'joao',
			role: 2,
			email: 'joao@fablab.org'
		});
	});

	it('mapMeToUser sem permission ativa usa o fallback (padrão 4)', async () => {
		const { mapMeToUser } = await freshApiAuth();
		const semAtiva: MeResponse = { ...ME, permissions: [] };

		expect(mapMeToUser(semAtiva)).toMatchObject({ id: '7', role: 4 });
		expect(mapMeToUser(semAtiva, 1)).toMatchObject({ role: 1 });
	});

	it('mapRoleNameToInt cobre os 5 nomes do backend', async () => {
		const { mapRoleNameToInt } = await freshApiAuth();

		expect([
			mapRoleNameToInt('ADMIN'),
			mapRoleNameToInt('BOLSISTA'),
			mapRoleNameToInt('VOLUNTARIO'),
			mapRoleNameToInt('ESTAGIARIO'),
			mapRoleNameToInt('RECRUTANDO')
		]).toEqual([0, 1, 2, 3, 4]);
	});
});
