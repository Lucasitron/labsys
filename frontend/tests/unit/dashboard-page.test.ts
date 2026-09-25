import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/svelte';
import { auth } from '$lib/stores/auth';
import DashboardPage from '../../src/routes/(app)/dashboard/+page.svelte';
import type { User } from '$lib/types/auth';
import type { DashboardSummary } from '$lib/types/dashboard';

const { fetchSummaryMock } = vi.hoisted(() => ({
	fetchSummaryMock: vi.fn()
}));

vi.mock('$lib/api/dashboard', () => ({
	fetchSummary: fetchSummaryMock,
	completeTask: vi.fn()
}));

vi.mock('$app/navigation', () => ({
	goto: vi.fn()
}));

const ADMIN: User = {
	id: 'u1',
	username: 'joao',
	name: 'João Silva',
	email: 'joao@fablab.org',
	role: 0
};

function fullSummary(): DashboardSummary {
	return {
		tasks: [
			{ id: 't1', title: 'Revisar estoque', module: 'estoque', dueLabel: 'Hoje', urgent: true }
		],
		kpis: {
			ordersActive: { value: 3, delta: '+2', deltaTone: 'success' },
			loansOpen: { value: 1, total: 8, restricted: true },
			machinesActive: { value: 2, total: 5, restricted: true },
			notificationsUnread: { value: 4 }
		},
		ordersByStatus: [{ status: 'em-producao', label: 'Em produção', count: 2, color: 'warn' }],
		machinesByStatus: [{ status: 'active', label: 'Ativas', count: 3, color: 'success' }],
		activity: [
			{
				id: 'a1',
				actor: 'João',
				verb: 'criou',
				target: '#1203',
				module: 'vendas',
				at: new Date().toISOString()
			}
		]
	};
}

function emptySummary(): DashboardSummary {
	return {
		tasks: [],
		kpis: { ordersActive: { value: 0 }, notificationsUnread: { value: 0 } },
		ordersByStatus: [],
		machinesByStatus: [],
		activity: []
	};
}

async function flush(): Promise<void> {
	await Promise.resolve();
	await Promise.resolve();
	await Promise.resolve();
}

beforeEach(() => {
	fetchSummaryMock.mockReset();
	auth.set({ user: null, token: null, isAuthenticated: false });
});

afterEach(() => {
	cleanup();
	fetchSummaryMock.mockReset();
	auth.set({ user: null, token: null, isAuthenticated: false });
});

describe('dashboard +page (estados)', () => {
	it('loading: fetch pendente renderiza skeletons sem blocos de dados', async () => {
		let resolveSummary!: (v: DashboardSummary) => void;
		fetchSummaryMock.mockImplementationOnce(
			() => new Promise<DashboardSummary>((resolve) => (resolveSummary = resolve))
		);
		auth.set({ user: ADMIN, token: 'tk', isAuthenticated: true });
		render(DashboardPage);

		await flush();

		expect(fetchSummaryMock).toHaveBeenCalledTimes(1);
		expect(screen.queryByText('Minhas tarefas')).toBeNull();
		expect(screen.queryByText('Encomendas ativas')).toBeNull();
		expect(document.querySelectorAll('[aria-hidden="true"]').length).toBeGreaterThan(0);

		resolveSummary(fullSummary());
		await waitFor(() => expect(screen.getByText('Minhas tarefas')).toBeTruthy());
	});

	it('sucesso: admin vê Greeting + MyTasks + KpiGrid + donuts + feed', async () => {
		fetchSummaryMock.mockResolvedValue(fullSummary());
		auth.set({ user: ADMIN, token: 'tk', isAuthenticated: true });
		render(DashboardPage);

		await waitFor(() => expect(screen.getByText('Minhas tarefas')).toBeTruthy());

		expect(screen.getByText(/^(Bom dia|Boa tarde|Boa noite), João\.$/)).toBeTruthy();
		expect(screen.getAllByTestId('kpi-card')).toHaveLength(4);
		expect(screen.getByText('Encomendas por status')).toBeTruthy();
		expect(screen.getByText('Status das máquinas')).toBeTruthy();
		expect(screen.getByText('Atividade recente')).toBeTruthy();
		expect(screen.getByText('Revisar estoque')).toBeTruthy();
	});

	it('empty: greeting "Bem-vindo, {nome}." e blocos empty', async () => {
		fetchSummaryMock.mockResolvedValue(emptySummary());
		auth.set({ user: ADMIN, token: 'tk', isAuthenticated: true });
		render(DashboardPage);

		await waitFor(() => expect(screen.getByText('Bem-vindo, João.')).toBeTruthy());
		expect(screen.getByText('Ainda não há dados para exibir. Comece por aqui:')).toBeTruthy();
		expect(screen.getByText('Sem tarefas por aqui')).toBeTruthy();
		expect(screen.getByText('Nenhuma atividade recente.')).toBeTruthy();
	});

	it('erro total: ErrorBanner global e "Tentar novamente" refaz o fetch', async () => {
		fetchSummaryMock.mockRejectedValueOnce(new Error('falhou'));
		fetchSummaryMock.mockResolvedValueOnce(fullSummary());
		auth.set({ user: ADMIN, token: 'tk', isAuthenticated: true });
		render(DashboardPage);

		await waitFor(() =>
			expect(screen.getByText('Não foi possível carregar o dashboard')).toBeTruthy()
		);

		const banner = screen.getByRole('alert');
		await fireEvent.click(within(banner).getByText('Tentar novamente'));

		await waitFor(() => expect(fetchSummaryMock).toHaveBeenCalledTimes(2));
		await waitFor(() => expect(screen.getByText('Minhas tarefas')).toBeTruthy());
	});

	it('visibilitychange com página visível dispara refetch do summary', async () => {
		fetchSummaryMock.mockResolvedValue(fullSummary());
		auth.set({ user: ADMIN, token: 'tk', isAuthenticated: true });
		render(DashboardPage);

		await waitFor(() => expect(fetchSummaryMock).toHaveBeenCalledTimes(1));

		document.dispatchEvent(new Event('visibilitychange'));

		await waitFor(() => expect(fetchSummaryMock).toHaveBeenCalledTimes(2));
	});

	it('sem canSeeMachines: MachinesChart ausente do DOM e OrdersChart com md:col-span-2', async () => {
		fetchSummaryMock.mockResolvedValue(fullSummary());
		auth.set({
			user: { ...ADMIN, role: 2, responsibilities: {} },
			token: 'tk',
			isAuthenticated: true
		});
		render(DashboardPage);

		await waitFor(() => expect(screen.getByText('Encomendas por status')).toBeTruthy());

		expect(screen.queryByText('Status das máquinas')).toBeNull();

		const ordersSection = screen.getByText('Encomendas por status').closest('section');
		expect(ordersSection?.className).toContain('md:col-span-2');
	});
});