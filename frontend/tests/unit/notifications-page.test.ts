import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/svelte';
import { page as notifPage, applyUrl, resetUrl } from './notif-page-state.svelte.js';
import NotifPage from '../../src/routes/(app)/notificacoes/+page.svelte';
import type { Notification, PagedNotifications } from '$lib/types/notifications';

const {
	listMock,
	markReadMock,
	markAllReadMock,
	gotoMock,
	toastSuccessMock,
	toastErrorMock,
	unreadCountStore
} = vi.hoisted(() => {
	const slist = vi.fn();
	const smarkRead = vi.fn();
	const smarkAll = vi.fn();
	const sgoto = vi.fn();
	const success = vi.fn();
	const err = vi.fn();

	let value = 0;
	const subs = new Set<(v: number) => void>();
	const store = {
		subscribe(fn: (v: number) => void) {
			fn(value);
			subs.add(fn);
			return () => subs.delete(fn);
		},
		set(v: number) {
			value = v;
			subs.forEach((fn) => fn(v));
		},
		get() {
			return value;
		}
	};
	return {
		listMock: slist,
		markReadMock: smarkRead,
		markAllReadMock: smarkAll,
		gotoMock: sgoto,
		toastSuccessMock: success,
		toastErrorMock: err,
		unreadCountStore: store
	};
});

vi.mock('$app/state', () => ({ page: notifPage }));
vi.mock('$app/navigation', () => ({ goto: gotoMock }));
vi.mock('$lib/stores/notifications', () => ({ unreadCount: unreadCountStore }));
vi.mock('$lib/stores/toast', () => ({
	toasts: { success: toastSuccessMock },
	toastError: toastErrorMock
}));
vi.mock('$lib/api/notifications', () => ({
	listNotifications: listMock,
	markRead: markReadMock,
	markAllRead: markAllReadMock,
	getPreferences: vi.fn(),
	savePreferences: vi.fn(),
	history: vi.fn()
}));

function notif(overrides: Partial<Notification> = {}): Notification {
	return {
		id: 'n1',
		type: 'sistema',
		title: 'Manutenção agendada',
		body: 'A fila da impressora foi liberada.',
		read: false,
		createdAt: '2026-09-21T10:00:00.000Z',
		...overrides
	};
}

function pagina(items: Notification[], totalPages = 1): PagedNotifications<Notification> {
	return { items, total: items.length, page: 1, pageSize: 10, totalPages };
}

async function flush(): Promise<void> {
	await Promise.resolve();
	await Promise.resolve();
	await Promise.resolve();
	await Promise.resolve();
}

function lastListCall(): Record<string, unknown> {
	return listMock.mock.calls.at(-1)![0] as Record<string, unknown>;
}

beforeEach(() => {
	resetUrl('');
	listMock.mockReset();
	markReadMock.mockReset();
	markAllReadMock.mockReset();
	gotoMock.mockReset();
	toastSuccessMock.mockReset();
	toastErrorMock.mockReset();
	unreadCountStore.set(0);
	gotoMock.mockImplementation((href: string) => {
		applyUrl(href);
		return Promise.resolve();
	});
});

afterEach(() => {
	cleanup();
	vi.useRealTimers();
});

describe('notificacoes +page (lista central)', () => {
	it('loading: fetch pendente renderiza skeleton sem rows', async () => {
		listMock.mockImplementationOnce(() => new Promise(() => undefined));
		render(NotifPage);

		await flush();

		expect(listMock).toHaveBeenCalledTimes(1);
		expect(screen.getByTestId('notifications-skeleton')).toBeTruthy();
		expect(screen.queryByTestId('notification-row')).toBeNull();
	});

	it('sucesso: renderiza rows com heartbeat local', async () => {
		listMock.mockResolvedValue(pagina([notif(), notif({ id: 'n2', type: 'estoque' })]));
		render(NotifPage);

		await waitFor(() => expect(screen.getAllByTestId('notification-row')).toHaveLength(2));
		expect(screen.getAllByText('Manutenção agendada')).toHaveLength(2);
		expect(screen.getByTestId('notifications-heartbeat').textContent).toMatch(/^Atualizado · /);
		expect(listMock).toHaveBeenCalledWith({
			page: 1,
			pageSize: 10,
			type: undefined,
			read: undefined
		});
	});

	it('empty sem filtros: EmptyState com CTA para o painel', async () => {
		listMock.mockResolvedValue(pagina([]));
		render(NotifPage);

		await waitFor(() => expect(screen.getByTestId('notifications-empty')).toBeTruthy());
		expect(screen.getByText('Nenhuma notificação por aqui')).toBeTruthy();
		const cta = screen.getByText('Ir para o painel');
		expect(cta.getAttribute('href')).toBe('/dashboard');
		expect(screen.queryByTestId('notifications-clear-filters')).toBeNull();
	});

	it('empty filtrado: chips ativos + limpar filtros refaz sem filtros', async () => {
		resetUrl('type=estoque&status=unread');
		listMock.mockResolvedValue(pagina([]));
		render(NotifPage);

		await waitFor(() => expect(screen.getByTestId('notifications-empty-filtered')).toBeTruthy());
		expect(screen.getByText('Nenhuma notificação encontrada')).toBeTruthy();
		expect(screen.getByText('Tipo: Estoque')).toBeTruthy();
		expect(screen.getByText('Status: Não lidas')).toBeTruthy();
		expect(lastListCall()).toMatchObject({ type: 'estoque', read: false });

		await fireEvent.click(
			within(screen.getByTestId('notifications-empty-filtered')).getByTestId(
				'notifications-clear-filters'
			)
		);

		await waitFor(() => expect(screen.getByTestId('notifications-empty')).toBeTruthy());
		expect(gotoMock).toHaveBeenCalledWith('/notificacoes', { keepFocus: true });
		expect(lastListCall()).toMatchObject({ type: undefined, read: undefined });
	});

	it('erro: banner + "Tentar novamente" refaz o fetch', async () => {
		listMock.mockRejectedValueOnce(new Error('Falha de rede.'));
		listMock.mockResolvedValueOnce(pagina([notif()]));
		render(NotifPage);

		await waitFor(() => expect(screen.getByTestId('notifications-error')).toBeTruthy());
		expect(screen.getByRole('alert').textContent).toContain('Falha de rede.');

		await fireEvent.click(screen.getByText('Tentar novamente'));

		await waitFor(() => expect(screen.getAllByTestId('notification-row')).toHaveLength(1));
		expect(listMock).toHaveBeenCalledTimes(2);
	});

	it('mark-read otimista decrementa contador e some com 0', async () => {
		unreadCountStore.set(1);
		listMock.mockResolvedValue(pagina([notif()]));
		markReadMock.mockResolvedValue(undefined);
		render(NotifPage);

		await waitFor(() => expect(screen.getByTestId('notification-mark-all-read')).toBeTruthy());
		await waitFor(() => expect(screen.getByTestId('notification-mark-read')).toBeTruthy());
		await fireEvent.click(screen.getByTestId('notification-mark-read'));

		expect(markReadMock).toHaveBeenCalledWith('n1');
		await waitFor(() => expect(screen.queryByTestId('notification-mark-all-read')).toBeNull());
		expect(screen.getByText('Lida')).toBeTruthy();
		expect(screen.queryByText('Não lida')).toBeNull();
	});

	it('mark-read falha: reverte otimismo e dispara toast de erro', async () => {
		unreadCountStore.set(1);
		listMock.mockResolvedValue(pagina([notif()]));
		markReadMock.mockRejectedValueOnce(new Error('boom'));
		render(NotifPage);

		await waitFor(() => expect(screen.getByTestId('notification-mark-read')).toBeTruthy());
		await fireEvent.click(screen.getByTestId('notification-mark-read'));

		await waitFor(() => expect(screen.getByTestId('notification-mark-read')).toBeTruthy());
		expect(screen.getByText('Não lida')).toBeTruthy();
		expect(screen.queryByText('Lida')).toBeNull();
		expect(screen.getByTestId('notification-mark-all-read')).toBeTruthy();
		expect(toastErrorMock).toHaveBeenCalledWith('Erro ao atualizar a notificação');
	});

	it('mark-all: marca todas como lidas, dispara toast de sucesso', async () => {
		unreadCountStore.set(2);
		listMock.mockResolvedValue(pagina([notif(), notif({ id: 'n2' })]));
		markAllReadMock.mockResolvedValue(undefined);
		render(NotifPage);

		await waitFor(() => expect(screen.getAllByTestId('notification-mark-read')).toHaveLength(2));
		await fireEvent.click(screen.getByTestId('notification-mark-all-read'));

		expect(markAllReadMock).toHaveBeenCalledTimes(1);
		await waitFor(() => expect(screen.getAllByText('Lida')).toHaveLength(2));
		expect(screen.queryAllByTestId('notification-mark-read')).toHaveLength(0);
		expect(toastSuccessMock).toHaveBeenCalledWith('Todas as notificações marcadas como lidas');
	});

	it('mark-all some quando contador é 0 e reaparece quando o store atualiza', async () => {
		unreadCountStore.set(0);
		listMock.mockResolvedValue(pagina([notif()]));
		render(NotifPage);

		await waitFor(() => expect(screen.getByTestId('notification-row')).toBeTruthy());
		expect(screen.queryByTestId('notification-mark-all-read')).toBeNull();

		unreadCountStore.set(3);
		await flush();

		expect(screen.getByTestId('notification-mark-all-read')).toBeTruthy();
	});

	it('heartbeat atualiza após mudança no store (timestamp local)', async () => {
		vi.useFakeTimers();
		vi.setSystemTime(new Date('2026-09-21T12:00:00Z'));
		listMock.mockResolvedValue(pagina([notif()]));
		render(NotifPage);

		await flush();
		expect(screen.getByTestId('notifications-heartbeat').textContent).toBe('Atualizado · agora');

		await vi.advanceTimersByTimeAsync(120_000);
		unreadCountStore.set(3);
		await flush();

		expect(screen.getByTestId('notifications-heartbeat').textContent).toBe('Atualizado · agora');
		expect(screen.getByTestId('notification-mark-all-read')).toBeTruthy();
	});

	it('pagination: página 2 via URL chama list com page nova', async () => {
		listMock.mockResolvedValue(pagina([notif()], 2));
		render(NotifPage);

		await waitFor(() => expect(screen.getByTestId('notifications-pagination')).toBeTruthy());
		await fireEvent.click(screen.getByLabelText('Próxima página'));

		expect(gotoMock).toHaveBeenCalledWith('/notificacoes?page=2', { keepFocus: true });
		await waitFor(() => expect(listMock).toHaveBeenCalledTimes(2));
		expect(lastListCall()).toMatchObject({ page: 2, pageSize: 10 });
	});

	it('filtro por aba "Não lidas" refaz list com read=false e "Todas" omite', async () => {
		listMock.mockResolvedValue(pagina([notif()]));
		render(NotifPage);

		await waitFor(() => expect(screen.getByTestId('notification-row')).toBeTruthy());

		await fireEvent.click(screen.getByRole('tab', { name: 'Não lidas' }));
		await waitFor(() => expect(listMock).toHaveBeenCalledTimes(2));
		expect(lastListCall()).toMatchObject({ read: false });
		expect(gotoMock).toHaveBeenCalledWith('/notificacoes?status=unread', { keepFocus: true });

		await fireEvent.click(screen.getByRole('tab', { name: 'Todas' }));
		await waitFor(() => expect(listMock).toHaveBeenCalledTimes(3));
		expect(lastListCall()).toMatchObject({ read: undefined });
	});
});