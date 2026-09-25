import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen } from '@testing-library/svelte';
import { auth } from '$lib/stores/auth';
import KpiGrid from '../../src/routes/(app)/dashboard/components/KpiGrid.svelte';
import type { User } from '$lib/types/auth';
import type { DashboardKpis } from '$lib/types/dashboard';

function user(overrides: Partial<User> = {}): User {
	return {
		id: 'u1',
		username: 'joao',
		name: 'João Silva',
		email: 'joao@fablab.org',
		role: 2,
		...overrides
	};
}

const KPIS: DashboardKpis = {
	ordersActive: { value: 3, delta: '+2', deltaTone: 'success' },
	loansOpen: { value: 1, total: 8, restricted: true },
	machinesActive: { value: 2, total: 5, restricted: true },
	notificationsUnread: { value: 4 }
};

beforeEach(() => {
	auth.set({ user: null, token: null, isAuthenticated: false });
});

afterEach(() => {
	cleanup();
	auth.set({ user: null, token: null, isAuthenticated: false });
});

function renderAs(u: User) {
	auth.set({ user: u, token: 'tk', isAuthenticated: true });
	render(KpiGrid, { props: { kpis: KPIS } });
}

describe('KpiGrid (RBAC por bloco)', () => {
	it('admin (role 0) renderiza os 4 cards, incluindo Empréstimos e Máquinas com badge RESP.', () => {
		renderAs(user({ role: 0 }));

		expect(screen.getAllByTestId('kpi-card')).toHaveLength(4);
		expect(screen.getByText('Encomendas ativas')).toBeTruthy();
		expect(screen.getByText('Empréstimos em aberto')).toBeTruthy();
		expect(screen.getByText('Máquinas ativas')).toBeTruthy();
		expect(screen.getByText('Notificações não lidas')).toBeTruthy();
		expect(screen.getAllByText('RESP.')).toHaveLength(2);
	});

	it('responsável por estoque vê Empréstimos e não vê Máquinas', () => {
		renderAs(user({ responsibilities: { estoque: ['cat_3d'] } }));

		expect(screen.getAllByTestId('kpi-card')).toHaveLength(3);
		expect(screen.getByText('Empréstimos em aberto')).toBeTruthy();
		expect(screen.queryByText('Máquinas ativas')).toBeNull();
		expect(screen.getAllByText('RESP.')).toHaveLength(1);
	});

	it('responsável por produção vê Máquinas e não vê Empréstimos', () => {
		renderAs(user({ responsibilities: { producao: ['laser'] } }));

		expect(screen.getAllByTestId('kpi-card')).toHaveLength(3);
		expect(screen.getByText('Máquinas ativas')).toBeTruthy();
		expect(screen.queryByText('Empréstimos em aberto')).toBeNull();
		expect(screen.getAllByText('RESP.')).toHaveLength(1);
	});

	it('sem role 0 nem responsabilidade: cards restritos ausentes do DOM (hide, não disable)', () => {
		renderAs(user());

		expect(screen.getAllByTestId('kpi-card')).toHaveLength(2);
		expect(screen.queryByText('Empréstimos em aberto')).toBeNull();
		expect(screen.queryByText('Máquinas ativas')).toBeNull();
		expect(screen.queryByText('RESP.')).toBeNull();
	});
});