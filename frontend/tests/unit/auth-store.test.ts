import { beforeEach, describe, expect, it, vi } from 'vitest';
import { get } from 'svelte/store';
import { ApiError } from '$lib/api/client';
import type { LoginResponse, MeResponse, Role, User } from '$lib/types/auth';

const STORAGE_KEY = 'fablab.auth';

const loginMock = vi.fn();
const fetchMeRawMock = vi.fn();

vi.mock('$lib/api/auth', async (importOriginal) => {
	const actual = await importOriginal<typeof import('$lib/api/auth')>();
	return { ...actual, login: loginMock, fetchMeRaw: fetchMeRawMock };
});

vi.mock('$app/environment', () => ({
	browser: true
}));

const SESSION: LoginResponse = {
	accessToken: 'access-1',
	refreshToken: 'refresh-1',
	tokenType: 'Bearer',
	expiresIn: 3600,
	idUser: 7,
	role: 'VOLUNTARIO',
	setor: null,
	nomeUsuario: 'joao'
};

const ME: MeResponse = {
	id: 99,
	idUser: 7,
	email: 'joao@fablab.org',
	nomeUsuario: 'joao',
	setor: null,
	permissions: [
		{ role: 2 as Role, label: 'Voluntário', active: true },
		{ role: 0 as Role, label: 'Admin', active: false }
	]
};

const USER: User = {
	id: '7',
	username: 'joao',
	name: 'joao',
	email: 'joao@fablab.org',
	role: 2
};

async function freshStore() {
	vi.resetModules();
	return await import('$lib/stores/auth');
}

beforeEach(() => {
	loginMock.mockReset();
	fetchMeRawMock.mockReset();
	loginMock.mockResolvedValue(SESSION);
	fetchMeRawMock.mockResolvedValue(ME);
	window.localStorage.clear();
});

describe('stores/auth', () => {
	it('login busca /me, monta o User e persiste tokens em fablab.auth', async () => {
		const { auth, login } = await freshStore();
		await login('joao', 'segredo');

		expect(loginMock).toHaveBeenCalledTimes(1);
		expect(loginMock).toHaveBeenCalledWith('joao', 'segredo');
		expect(fetchMeRawMock).toHaveBeenCalledTimes(1);
		expect(fetchMeRawMock).toHaveBeenCalledWith('access-1');
		expect(get(auth)).toEqual({
			user: USER,
			token: 'access-1',
			refreshToken: 'refresh-1',
			isAuthenticated: true
		});
		expect(JSON.parse(window.localStorage.getItem(STORAGE_KEY) as string)).toEqual({
			user: USER,
			token: 'access-1',
			refreshToken: 'refresh-1'
		});
	});

	it('login sem permission ativa usa o role NOME da sessão como fallback', async () => {
		fetchMeRawMock.mockResolvedValue({ ...ME, permissions: [] });

		const { auth, login } = await freshStore();
		await login('joao', 'segredo');

		expect(get(auth).user).toEqual({ ...USER, role: 2 });
	});

	it('login com erro propaga e não seta estado autenticado', async () => {
		loginMock.mockRejectedValue(new ApiError(401, 'INVALID_CREDENTIALS', 'credenciais'));

		const { auth, login } = await freshStore();
		await expect(login('joao', 'errada')).rejects.toBeInstanceOf(ApiError);

		expect(get(auth)).toEqual({
			user: null,
			token: null,
			refreshToken: null,
			isAuthenticated: false
		});
		expect(window.localStorage.getItem(STORAGE_KEY)).toBeNull();
	});

	it('falha no /me após login propaga sem autenticar nem persistir', async () => {
		fetchMeRawMock.mockRejectedValue(new ApiError(401, 'EXPIRED', 'expirou'));

		const { auth, login } = await freshStore();
		await expect(login('joao', 'segredo')).rejects.toBeInstanceOf(ApiError);

		expect(get(auth).isAuthenticated).toBe(false);
		expect(get(auth).user).toBeNull();
		expect(window.localStorage.getItem(STORAGE_KEY)).toBeNull();
	});

	it('logout limpa a store e remove a chave do localStorage', async () => {
		const { auth, login, logout } = await freshStore();
		await login('joao', 'segredo');
		expect(get(auth).isAuthenticated).toBe(true);

		logout();

		expect(get(auth)).toEqual({
			user: null,
			token: null,
			refreshToken: null,
			isAuthenticated: false
		});
		expect(window.localStorage.getItem(STORAGE_KEY)).toBeNull();
	});

	it('setUser atualiza o usuário e re-persiste preservando os tokens', async () => {
		const { auth, login, setUser } = await freshStore();
		await login('joao', 'segredo');

		const updated: User = { ...USER, name: 'João S. Lima' };
		setUser(updated);

		expect(get(auth).user).toEqual(updated);
		expect(get(auth).token).toBe('access-1');
		expect(get(auth).refreshToken).toBe('refresh-1');
		expect(JSON.parse(window.localStorage.getItem(STORAGE_KEY) as string)).toEqual({
			user: updated,
			token: 'access-1',
			refreshToken: 'refresh-1'
		});
	});

	it('restaura sessão válida do localStorage', async () => {
		window.localStorage.setItem(
			STORAGE_KEY,
			JSON.stringify({ user: USER, token: 'access-1', refreshToken: 'refresh-1' })
		);

		const { auth } = await freshStore();

		expect(get(auth)).toEqual({
			user: USER,
			token: 'access-1',
			refreshToken: 'refresh-1',
			isAuthenticated: true
		});
	});

	it('restore compatível com formato antigo (sem refreshToken) não desloga', async () => {
		window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ user: USER, token: 'access-1' }));

		const { auth } = await freshStore();

		expect(get(auth)).toEqual({
			user: USER,
			token: 'access-1',
			refreshToken: null,
			isAuthenticated: true
		});
	});

	it('restore com JSON corrompido resulta em estado vazio sem crash', async () => {
		window.localStorage.setItem(STORAGE_KEY, '{corrompido');

		const { auth } = await freshStore();

		expect(get(auth)).toEqual({
			user: null,
			token: null,
			refreshToken: null,
			isAuthenticated: false
		});
	});

	it('restore sem chave resulta em estado vazio', async () => {
		const { auth } = await freshStore();

		expect(get(auth)).toEqual({
			user: null,
			token: null,
			refreshToken: null,
			isAuthenticated: false
		});
	});

	it('restore não cria estado inconsistente (token sem user)', async () => {
		window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ user: null, token: 'access-1' }));

		const { auth } = await freshStore();

		expect(get(auth)).toEqual({
			user: null,
			token: null,
			refreshToken: null,
			isAuthenticated: false
		});
	});
});
