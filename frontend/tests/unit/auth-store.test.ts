import { beforeEach, describe, expect, it, vi } from 'vitest';
import { get } from 'svelte/store';
import { ApiError } from '$lib/api/client';
import type { User } from '$lib/types/auth';

const STORAGE_KEY = 'fablab.auth';

const loginMock = vi.fn();

vi.mock('$lib/api/auth', () => ({
	login: loginMock
}));

vi.mock('$app/environment', () => ({
	browser: true
}));

const USER: User = {
	id: 'u1',
	username: 'joao',
	name: 'João Silva',
	email: 'joao@fablab.org',
	role: 2
};

async function freshStore() {
	vi.resetModules();
	return await import('$lib/stores/auth');
}

beforeEach(() => {
	loginMock.mockReset();
	window.localStorage.clear();
});

describe('stores/auth', () => {
	it('login chama a api, seta estado autenticado e persiste em fablab.auth', async () => {
		loginMock.mockResolvedValue({ token: 'token-1', expiresIn: 3600, user: USER });

		const { auth, login } = await freshStore();
		await login('joao', 'segredo');

		expect(loginMock).toHaveBeenCalledTimes(1);
		expect(loginMock).toHaveBeenCalledWith('joao', 'segredo');
		expect(get(auth)).toEqual({ user: USER, token: 'token-1', isAuthenticated: true });
		expect(window.localStorage.getItem(STORAGE_KEY)).toBe(
			JSON.stringify({ user: USER, token: 'token-1' })
		);
	});

	it('login com erro propaga e não seta estado autenticado', async () => {
		loginMock.mockRejectedValue(new ApiError(401, 'INVALID_CREDENTIALS', 'credenciais'));

		const { auth, login } = await freshStore();
		await expect(login('joao', 'errada')).rejects.toBeInstanceOf(ApiError);

		expect(get(auth)).toEqual({ user: null, token: null, isAuthenticated: false });
		expect(window.localStorage.getItem(STORAGE_KEY)).toBeNull();
	});

	it('logout limpa a store e remove a chave do localStorage', async () => {
		loginMock.mockResolvedValue({ token: 'token-1', expiresIn: 3600, user: USER });

		const { auth, login, logout } = await freshStore();
		await login('joao', 'segredo');
		expect(get(auth).isAuthenticated).toBe(true);

		logout();

		expect(get(auth)).toEqual({ user: null, token: null, isAuthenticated: false });
		expect(window.localStorage.getItem(STORAGE_KEY)).toBeNull();
	});

	it('setUser atualiza o usuário e re-persiste', async () => {
		loginMock.mockResolvedValue({ token: 'token-1', expiresIn: 3600, user: USER });

		const { auth, login, setUser } = await freshStore();
		await login('joao', 'segredo');

		const updated: User = { ...USER, name: 'João S. Lima' };
		setUser(updated);

		expect(get(auth).user).toEqual(updated);
		expect(get(auth).token).toBe('token-1');
		expect(window.localStorage.getItem(STORAGE_KEY)).toBe(
			JSON.stringify({ user: updated, token: 'token-1' })
		);
	});

	it('restaura sessão válida do localStorage', async () => {
		window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ user: USER, token: 'token-1' }));

		const { auth } = await freshStore();

		expect(get(auth)).toEqual({ user: USER, token: 'token-1', isAuthenticated: true });
	});

	it('restore com JSON corrompido resulta em estado vazio sem crash', async () => {
		window.localStorage.setItem(STORAGE_KEY, '{corrompido');

		const { auth } = await freshStore();

		expect(get(auth)).toEqual({ user: null, token: null, isAuthenticated: false });
	});

	it('restore sem chave resulta em estado vazio', async () => {
		const { auth } = await freshStore();

		expect(get(auth)).toEqual({ user: null, token: null, isAuthenticated: false });
	});

	it('restore não cria estado inconsistente (token sem user)', async () => {
		window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ user: null, token: 'token-1' }));

		const { auth } = await freshStore();

		expect(get(auth)).toEqual({ user: null, token: null, isAuthenticated: false });
	});
});