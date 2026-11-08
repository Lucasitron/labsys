import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/svelte';
import PrefsPage from '../../src/routes/(app)/notificacoes/preferencias/+page.svelte';
import { NOTIFICATION_TYPES } from '$lib/utils/notification-format';
import type { NotificationPreferences, NotificationType } from '$lib/types/notifications';

const { getPrefsMock, savePrefsMock, toastSuccessMock } = vi.hoisted(() => ({
	getPrefsMock: vi.fn(),
	savePrefsMock: vi.fn(),
	toastSuccessMock: vi.fn()
}));

vi.mock('$lib/api/notifications', () => ({
	getPreferences: getPrefsMock,
	savePreferences: savePrefsMock
}));

vi.mock('$lib/stores/toast', () => ({
	toasts: { success: toastSuccessMock },
	toastError: vi.fn()
}));

function matriz(sistemaPush: boolean): NotificationPreferences {
	const m = {} as NotificationPreferences;
	for (const tipo of NOTIFICATION_TYPES) {
		m[tipo] = { inapp: true, email: true, push: tipo === 'sistema' ? sistemaPush : true };
	}
	return m;
}

async function flush(): Promise<void> {
	await Promise.resolve();
	await Promise.resolve();
	await Promise.resolve();
}

function saveButton(): HTMLButtonElement {
	return screen.getByTestId('notif-pref-save') as HTMLButtonElement;
}

function toggles(): HTMLInputElement[] {
	return screen.getAllByTestId('notif-pref-toggle') as HTMLInputElement[];
}

beforeEach(() => {
	getPrefsMock.mockReset();
	savePrefsMock.mockReset();
	toastSuccessMock.mockReset();
	savePrefsMock.mockResolvedValue(undefined);
});

afterEach(() => {
	cleanup();
});

describe('notificacoes/preferencias +page (matriz de canais)', () => {
	it('render: matriz tipo×canal com 18 toggles, lista de canais e pill Admin', async () => {
		getPrefsMock.mockResolvedValue(matriz(false));
		render(PrefsPage);

		await waitFor(() => expect(toggles()).toHaveLength(18));

		expect(screen.getByText('Restrito ao Admin')).toBeTruthy();
		const list = screen.getByTestId('notif-pref-list');
		expect(within(list).getByText('In-app')).toBeTruthy();
		expect(within(list).getByText('E-mail')).toBeTruthy();
		expect(within(list).getByText('Push')).toBeTruthy();
		expect(screen.getByText('Encomenda')).toBeTruthy();
		expect(screen.getByText('Sistema')).toBeTruthy();
	});

	it('toggle marca dirty e habilita/desabilita o botão salvar', async () => {
		getPrefsMock.mockResolvedValue(matriz(false));
		render(PrefsPage);
		await waitFor(() => expect(toggles()).toHaveLength(18));

		expect(saveButton().disabled).toBe(true);

		await fireEvent.click(toggles()[0]);
		await flush();
		expect(toggles()[0].checked).toBe(false);
		expect(saveButton().disabled).toBe(false);

		await fireEvent.click(toggles()[0]);
		await flush();
		expect(toggles()[0].checked).toBe(true);
		expect(saveButton().disabled).toBe(true);
	});

	it('salvar chama savePreferences com a matriz atual e limpa dirty', async () => {
		getPrefsMock.mockResolvedValue(matriz(false));
		render(PrefsPage);
		await waitFor(() => expect(toggles()).toHaveLength(18));

		await fireEvent.click(toggles()[0]);
		await fireEvent.click(saveButton());

		await waitFor(() => expect(savePrefsMock).toHaveBeenCalledTimes(1));
		const draft = savePrefsMock.mock.calls[0]![0] as NotificationPreferences;
		expect(draft.encomenda.inapp).toBe(false);
		expect(draft.sistema.push).toBe(false);

		await waitFor(() => expect(saveButton().disabled).toBe(true));
		expect(toastSuccessMock).toHaveBeenCalledWith('Preferências de canais salvas.');
	});

	it('falha ao salvar: banner de erro e valores mantidos no draft', async () => {
		getPrefsMock.mockResolvedValue(matriz(false));
		savePrefsMock.mockRejectedValueOnce(new Error('Erro de rede.'));
		render(PrefsPage);
		await waitFor(() => expect(toggles()).toHaveLength(18));

		await fireEvent.click(toggles()[1]);
		await fireEvent.click(saveButton());

		const banner = await waitFor(() => screen.getByTestId('notif-pref-save-error'));
		expect(banner.textContent).toContain('Suas alterações foram mantidas.');
		expect(toggles()[1].checked).toBe(false);
		expect(saveButton().disabled).toBe(false);
		expect(toastSuccessMock).not.toHaveBeenCalled();
	});

	it('"Restaurar padrão" deixa dirty (push de sistema OFF) mesmo partindo de tudo ON', async () => {
		getPrefsMock.mockResolvedValue(matriz(true));
		render(PrefsPage);
		await waitFor(() => expect(toggles()).toHaveLength(18));

		expect(toggles()[17].checked).toBe(true);
		expect(saveButton().disabled).toBe(true);

		await fireEvent.click(screen.getByTestId('notif-pref-reset'));
		await flush();

		expect((screen.getAllByTestId('notif-pref-toggle')[17] as HTMLInputElement).checked).toBe(false);
		expect(saveButton().disabled).toBe(false);
	});

	it('pill "Restrito ao Admin" presente no header', async () => {
		getPrefsMock.mockResolvedValue(matriz(false));
		render(PrefsPage);
		await waitFor(() => expect(screen.getByText('Restrito ao Admin')).toBeTruthy());
	});
});