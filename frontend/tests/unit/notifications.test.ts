import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { get } from 'svelte/store';

const { fetchUnreadCountMock } = vi.hoisted(() => ({
	fetchUnreadCountMock: vi.fn()
}));

vi.mock('$lib/api/dashboard', () => ({
	fetchUnreadCount: fetchUnreadCountMock
}));

vi.mock('$app/environment', () => ({
	browser: true
}));

let storeRef: { stopPolling: () => void } | null = null;

async function freshStore() {
	vi.resetModules();
	const mod = await import('$lib/stores/notifications');
	storeRef = mod;
	return mod;
}

function setHidden(hidden: boolean): void {
	Object.defineProperty(document, 'hidden', { configurable: true, value: hidden });
}

async function flush(): Promise<void> {
	await Promise.resolve();
	await Promise.resolve();
	await Promise.resolve();
}

beforeEach(() => {
	vi.useFakeTimers();
	fetchUnreadCountMock.mockReset();
	fetchUnreadCountMock.mockResolvedValue({ count: 0 });
	setHidden(false);
});

afterEach(() => {
	storeRef?.stopPolling();
	storeRef = null;
	vi.useRealTimers();
	setHidden(false);
	fetchUnreadCountMock.mockReset();
});

describe('stores/notifications (polling)', () => {
	it('startPolling faz refresh imediato e atualiza o contador', async () => {
		fetchUnreadCountMock.mockResolvedValue({ count: 3 });
		const { startPolling, unreadCount } = await freshStore();

		startPolling();
		await flush();

		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(1);
		expect(get(unreadCount)).toBe(3);
	});

	it('a cada 60s dispara um novo refresh', async () => {
		const { startPolling } = await freshStore();

		startPolling();
		await flush();
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(1);

		await vi.advanceTimersByTimeAsync(60_000);
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(2);

		await vi.advanceTimersByTimeAsync(60_000);
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(3);
	});

	it('aba oculta (document.hidden) pausa o polling', async () => {
		setHidden(true);
		const { startPolling } = await freshStore();

		startPolling();
		await flush();
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(1);

		await vi.advanceTimersByTimeAsync(180_000);
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(1);
	});

	it('visibilitychange ao voltar de aba oculta faz refresh imediato e re-agenda', async () => {
		setHidden(true);
		const { startPolling } = await freshStore();

		startPolling();
		await flush();
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(1);

		await vi.advanceTimersByTimeAsync(120_000);
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(1);

		setHidden(false);
		document.dispatchEvent(new Event('visibilitychange'));
		await flush();
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(2);

		await vi.advanceTimersByTimeAsync(60_000);
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(3);
	});

	it('visibilitychange com página visível faz refresh imediato e re-agenda', async () => {
		const { startPolling } = await freshStore();

		startPolling();
		await flush();
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(1);

		await vi.advanceTimersByTimeAsync(60_000);
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(2);

		document.dispatchEvent(new Event('visibilitychange'));
		await flush();
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(3);

		await vi.advanceTimersByTimeAsync(60_000);
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(4);
	});

	it('stopPolling limpa timer e listener (sem refresh após destroy)', async () => {
		const { startPolling, stopPolling } = await freshStore();

		startPolling();
		await flush();
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(1);

		stopPolling();
		await vi.advanceTimersByTimeAsync(120_000);
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(1);

		document.dispatchEvent(new Event('visibilitychange'));
		await flush();
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(1);
	});

	it('startPolling duas vezes não cria instância duplicada', async () => {
		const { startPolling } = await freshStore();

		startPolling();
		startPolling();
		await flush();
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(1);

		await vi.advanceTimersByTimeAsync(60_000);
		expect(fetchUnreadCountMock).toHaveBeenCalledTimes(2);
	});

	it('falha no fetchUnreadCount mantém contador anterior sem crash', async () => {
		fetchUnreadCountMock.mockResolvedValueOnce({ count: 5 });
		const { startPolling, unreadCount } = await freshStore();

		startPolling();
		await flush();
		expect(get(unreadCount)).toBe(5);

		fetchUnreadCountMock.mockRejectedValueOnce(new Error('boom'));
		await vi.advanceTimersByTimeAsync(60_000);
		expect(get(unreadCount)).toBe(5);
	});
});