import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/svelte';
import HistoryPage from '../../src/routes/(app)/notificacoes/historico/+page.svelte';

const { historyMock } = vi.hoisted(() => ({ historyMock: vi.fn() }));

vi.mock('$lib/api/notifications', () => ({
	history: historyMock,
	getPreferences: vi.fn(),
	savePreferences: vi.fn(),
	listNotifications: vi.fn(),
	markRead: vi.fn(),
	markAllRead: vi.fn()
}));

function entry(overrides: Record<string, unknown> = {}) {
	return {
		id: 'h1',
		title: 'Notificação de teste',
		type: 'encomenda',
		recipient: 'João Silva',
		channel: 'email',
		read: true,
		sentAt: '2026-09-21T10:30:00.000Z',
		...overrides
	};
}

function payload(rows: unknown[]) {
	return { items: rows, total: rows.length };
}

async function flush(): Promise<void> {
	await Promise.resolve();
	await Promise.resolve();
	await Promise.resolve();
}

async function sleep(ms: number): Promise<void> {
	await new Promise((resolve) => setTimeout(resolve, ms));
}

function lastHistoryCall(): Record<string, unknown> {
	return historyMock.mock.calls.at(-1)![0] as Record<string, unknown>;
}

beforeEach(() => {
	historyMock.mockReset();
});

afterEach(() => {
	cleanup();
});

describe('notificacoes/historico +page (auditoria)', () => {
	it('render: tabela com rows, badges de canal e status', async () => {
		historyMock.mockResolvedValue(
			payload([entry(), entry({ id: 'h2', title: 'Estoque baixo', type: 'estoque', recipient: 'Maria Lima', read: false, channel: 'push' })])
		);
		render(HistoryPage);

		await waitFor(() => expect(screen.getAllByTestId('history-row')).toHaveLength(2));
		expect(screen.getByText('Notificação de teste')).toBeTruthy();
		expect(screen.getByText('Estoque baixo')).toBeTruthy();
		expect(screen.getByText('João Silva')).toBeTruthy();
		expect(screen.getByText('Maria Lima')).toBeTruthy();
		expect(screen.getAllByText('Encomenda').length).toBeGreaterThan(0);
		expect(screen.getAllByText('Estoque').length).toBeGreaterThan(0);
		expect(screen.getAllByText('E-mail').length).toBeGreaterThan(0);
		expect(screen.getAllByText('Push').length).toBeGreaterThan(0);
		expect(screen.getAllByText('Lida').length).toBeGreaterThan(0);
		expect(screen.getAllByText('Não lida').length).toBeGreaterThan(0);
		expect(screen.getAllByText(/21\/09\/2026/).length).toBeGreaterThan(0);
	});

	it('seletor de tipo altera a chamada de history com o filtro', async () => {
		historyMock.mockResolvedValue(payload([entry()]));
		render(HistoryPage);
		await waitFor(() => expect(screen.getByTestId('history-table')).toBeTruthy());

		await fireEvent.change(screen.getByLabelText('Tipo'), { target: { value: 'estoque' } });

		await waitFor(() => expect(historyMock).toHaveBeenCalledTimes(2));
		expect(lastHistoryCall()).toEqual({ type: 'estoque' });
		expect(screen.getByLabelText('Remover filtro Estoque')).toBeTruthy();
	});

	it('seletor de status "Não lidas" envia read=false', async () => {
		historyMock.mockResolvedValue(payload([entry()]));
		render(HistoryPage);
		await waitFor(() => expect(screen.getByTestId('history-table')).toBeTruthy());

		await fireEvent.change(screen.getByLabelText('Status'), { target: { value: 'unread' } });

		await waitFor(() => expect(historyMock).toHaveBeenCalledTimes(2));
		expect(lastHistoryCall()).toEqual({ read: false });
	});

	it('busca com debounce altera history com search após digitar', async () => {
		historyMock.mockResolvedValue(payload([entry()]));
		render(HistoryPage);
		await waitFor(() => expect(screen.getByTestId('history-table')).toBeTruthy());

		await fireEvent.input(screen.getByLabelText('Buscar no histórico'), { target: { value: 'filamento' } });
		await sleep(420);

		await waitFor(() => expect(historyMock).toHaveBeenCalledTimes(2));
		expect(lastHistoryCall()).toEqual({ search: 'filamento' });
	});

	it('seletor de canal e período combinam filtros na chamada', async () => {
		historyMock.mockResolvedValue(payload([entry()]));
		render(HistoryPage);
		await waitFor(() => expect(screen.getByTestId('history-table')).toBeTruthy());

		await fireEvent.change(screen.getByLabelText('Canal'), { target: { value: 'push' } });
		await flush();
		await fireEvent.change(screen.getByLabelText('Período'), { target: { value: '7d' } });
		await flush();

		expect(lastHistoryCall()).toEqual({ channel: 'push', period: '7d' });
	});

	it('empty filtrado: chips + limpar filtros re-executa e restaura tabela', async () => {
		historyMock.mockResolvedValueOnce(payload([entry()]));
		historyMock.mockResolvedValueOnce(payload([]));
		historyMock.mockResolvedValue(payload([entry()]));
		render(HistoryPage);
		await waitFor(() => expect(screen.getByTestId('history-table')).toBeTruthy());

		await fireEvent.change(screen.getByLabelText('Tipo'), { target: { value: 'financeiro' } });

		await waitFor(() => expect(screen.getByTestId('history-empty-filtered')).toBeTruthy());
		expect(screen.getByText('Nenhum envio encontrado')).toBeTruthy();
		expect(screen.getByLabelText('Remover filtro Financeiro')).toBeTruthy();

		await fireEvent.click(screen.getByTestId('history-clear-filters'));

		await waitFor(() => expect(screen.getByTestId('history-table')).toBeTruthy());
		expect(historyMock).toHaveBeenCalledTimes(3);
		expect(lastHistoryCall()).toEqual({});
	});

	it('erro: banner + "Tentar novamente" refaz o fetch', async () => {
		historyMock.mockRejectedValueOnce(new Error('Falha de rede.'));
		historyMock.mockResolvedValueOnce(payload([entry()]));
		render(HistoryPage);

		await waitFor(() => expect(screen.getByTestId('history-error')).toBeTruthy());
		expect(screen.getByRole('alert').textContent).toContain('Falha de rede.');

		await fireEvent.click(screen.getByText('Tentar novamente'));

		await waitFor(() => expect(screen.getByTestId('history-table')).toBeTruthy());
		expect(historyMock).toHaveBeenCalledTimes(2);
	});

	it('paginação client-side: 12 envios → 10 na página 1, 2 na página 2, sem novo fetch', async () => {
		historyMock.mockResolvedValue(
			payload(Array.from({ length: 12 }, (_, i) => entry({ id: `h${i}`, title: `Envio ${i + 1}` })))
		);
		render(HistoryPage);

		await waitFor(() => expect(screen.getAllByTestId('history-row')).toHaveLength(10));
		expect(screen.getByText(/página 1 de 2 · 12 envios/)).toBeTruthy();

		await fireEvent.click(screen.getByLabelText('Próxima página'));

		await waitFor(() => expect(screen.getAllByTestId('history-row')).toHaveLength(2));
		expect(screen.getByText('Envio 11')).toBeTruthy();
		expect(screen.getByText('Envio 12')).toBeTruthy();
		expect(historyMock).toHaveBeenCalledTimes(1);
	});
});