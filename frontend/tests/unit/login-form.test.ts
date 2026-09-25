import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/svelte';
import { ApiError } from '$lib/api/client';
import LoginForm from '../../src/routes/auth/login/login-form.svelte';

const { loginMock } = vi.hoisted(() => ({ loginMock: vi.fn() }));

vi.mock('$lib/stores/auth', () => ({
	login: loginMock
}));

vi.mock('$app/navigation', () => ({
	goto: vi.fn()
}));

afterEach(() => {
	cleanup();
	loginMock.mockReset();
});

beforeEach(() => {
	loginMock.mockImplementation(() => Promise.resolve());
});

async function fillValid(): Promise<{ username: HTMLInputElement; password: HTMLInputElement }> {
	const username = screen.getByLabelText('Usuário') as HTMLInputElement;
	const password = screen.getByLabelText('Senha') as HTMLInputElement;
	await fireEvent.input(username, { target: { value: 'joao' } });
	await fireEvent.input(password, { target: { value: '123456' } });
	return { username, password };
}

describe('login-form', () => {
	it('submit vazio exibe erros de campo e não chama a api', async () => {
		const onsuccess = vi.fn();
		render(LoginForm, { props: { onsuccess } });

		await fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

		expect(screen.getByText('Informe seu usuário')).toBeTruthy();
		expect(screen.getByText('Informe sua senha')).toBeTruthy();
		expect(loginMock).not.toHaveBeenCalled();
		expect(onsuccess).not.toHaveBeenCalled();
	});

	it('senha menor que 6 exibe erro de campo', async () => {
		render(LoginForm, { props: { onsuccess: vi.fn() } });

		await fireEvent.input(screen.getByLabelText('Usuário') as HTMLInputElement, {
			target: { value: 'joao' }
		});
		await fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

		expect(screen.getByText('Informe sua senha')).toBeTruthy();
		expect(loginMock).not.toHaveBeenCalled();
	});

	it('submit válido chama login uma única vez e dispara onsuccess', async () => {
		const onsuccess = vi.fn();
		render(LoginForm, { props: { onsuccess } });

		await fillValid();
		await fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

		await waitFor(() => expect(onsuccess).toHaveBeenCalledTimes(1));
		expect(loginMock).toHaveBeenCalledTimes(1);
		expect(loginMock).toHaveBeenCalledWith('joao', '123456');
	});

	it('erro de login exibe role=alert e mantém campos preenchidos', async () => {
		loginMock.mockRejectedValue(new ApiError(401, 'INVALID_CREDENTIALS', 'credenciais'));
		render(LoginForm, { props: { onsuccess: vi.fn() } });

		await fillValid();
		await fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

		const alert = await waitFor(() => screen.getByRole('alert'));
		expect(alert.textContent).toContain('Usuário ou senha inválidos');

		expect((screen.getByLabelText('Usuário') as HTMLInputElement).value).toBe('joao');
		expect((screen.getByLabelText('Senha') as HTMLInputElement).value).toBe('123456');
		expect(loginMock).toHaveBeenCalledTimes(1);
	});

	it('durante loading desabilita botão, inputs, olho e link, exibindo Autenticando...', async () => {
		let resolveLogin: () => void = () => undefined;
		loginMock.mockImplementation(
			() => new Promise<void>((resolve) => (resolveLogin = resolve))
		);
		const onsuccess = vi.fn();
		render(LoginForm, { props: { onsuccess } });

		await fillValid();
		await fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

		const submit = screen.getByRole('button', { name: /Autenticando/ });
		expect((submit as HTMLButtonElement).disabled).toBe(true);
		expect(screen.getByText('Autenticando...')).toBeTruthy();
		expect((screen.getByLabelText('Usuário') as HTMLInputElement).disabled).toBe(true);
		expect((screen.getByLabelText('Senha') as HTMLInputElement).disabled).toBe(true);
		expect((screen.getByRole('button', { name: 'Mostrar senha' }) as HTMLButtonElement).disabled).toBe(
			true
		);
		expect(screen.getByText('Esqueci minha senha').getAttribute('aria-disabled')).toBe('true');

		resolveLogin();
		await waitFor(() => expect(onsuccess).toHaveBeenCalledTimes(1));
	});

	it('toggle do olho alterna type e aria-label', async () => {
		render(LoginForm, { props: { onsuccess: vi.fn() } });

		const password = screen.getByLabelText('Senha') as HTMLInputElement;
		expect(password.type).toBe('password');

		await fireEvent.click(screen.getByRole('button', { name: 'Mostrar senha' }));
		expect(password.type).toBe('text');
		expect(screen.getByRole('button', { name: 'Ocultar senha' })).toBeTruthy();

		await fireEvent.click(screen.getByRole('button', { name: 'Ocultar senha' }));
		expect(password.type).toBe('password');
		expect(screen.getByRole('button', { name: 'Mostrar senha' })).toBeTruthy();
	});

	it('inputs possuem autocomplete correto', async () => {
		render(LoginForm, { props: { onsuccess: vi.fn() } });

		expect(screen.getByLabelText('Usuário').getAttribute('autocomplete')).toBe('username');
		expect(screen.getByLabelText('Senha').getAttribute('autocomplete')).toBe('current-password');
	});

	it('Enter dentro do formulário dispara o submit', async () => {
		const onsuccess = vi.fn();
		render(LoginForm, { props: { onsuccess } });

		const { password } = await fillValid();
		await fireEvent.keyDown(password, { key: 'Enter', code: 'Enter' });

		await waitFor(() => expect(loginMock).toHaveBeenCalledTimes(1));
		expect(loginMock).toHaveBeenCalledWith('joao', '123456');
		await waitFor(() => expect(onsuccess).toHaveBeenCalledTimes(1));
	});
});