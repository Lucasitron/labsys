import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { writable } from 'svelte/store';
import type { AuthState } from '$lib/stores/auth';

const authMock = writable<AuthState>({
	user: null,
	token: 'tk-1',
	refreshToken: null,
	isAuthenticated: true
});

vi.mock('$lib/stores/auth', () => ({
	auth: authMock
}));

vi.mock('$app/environment', () => ({
	browser: true
}));

vi.mock('$app/navigation', () => ({
	goto: vi.fn()
}));

function jsonResponse(body: unknown, status = 200): Response {
	return new Response(JSON.stringify(body), {
		status,
		headers: { 'Content-Type': 'application/json' }
	});
}

async function freshSenha() {
	vi.resetModules();
	return await import('$lib/api/configuracoes/senha');
}

beforeEach(() => {
	vi.stubEnv('VITE_API_BASE_URL', 'http://api.test');
	authMock.set({ user: null, token: 'tk-1', refreshToken: null, isAuthenticated: true });
});

afterEach(() => {
	vi.unstubAllEnvs();
	vi.unstubAllGlobals();
});

describe('api/configuracoes/senha (contrato Laravel)', () => {
	it('alterarSenha envia PUT /auth/senha com os 3 campos (inclui confirmacaoSenha)', async () => {
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ mensagem: 'ok' }));
		vi.stubGlobal('fetch', fetchMock);
		const { alterarSenha } = await freshSenha();

		await alterarSenha('atual123', 'novaSenha123', 'novaSenha123');

		expect(fetchMock).toHaveBeenCalledTimes(1);
		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/auth/senha');
		expect(init!.method).toBe('PUT');
		expect((init!.headers as Headers).get('Authorization')).toBe('Bearer tk-1');
		expect(init!.body).toBe(
			JSON.stringify({
				senhaAtual: 'atual123',
				novaSenha: 'novaSenha123',
				confirmacaoSenha: 'novaSenha123'
			})
		);
	});

	it('confirmação divergente lança erro sem chamar a api', async () => {
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ mensagem: 'ok' }));
		vi.stubGlobal('fetch', fetchMock);
		const { alterarSenha } = await freshSenha();

		await expect(alterarSenha('atual123', 'novaSenha123', 'outra')).rejects.toThrow(
			'A nova senha e a confirmação não coincidem.'
		);
		expect(fetchMock).not.toHaveBeenCalled();
	});

	it('nova senha curta lança erro sem chamar a api', async () => {
		const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ mensagem: 'ok' }));
		vi.stubGlobal('fetch', fetchMock);
		const { alterarSenha } = await freshSenha();

		await expect(alterarSenha('atual123', 'curta', 'curta')).rejects.toThrow(
			'A nova senha deve ter pelo menos 8 caracteres.'
		);
		expect(fetchMock).not.toHaveBeenCalled();
	});
});
