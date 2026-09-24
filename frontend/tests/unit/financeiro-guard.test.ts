import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { User } from '$lib/types/auth';
import { load } from '../../src/routes/(app)/financeiro/+layout';
import { EDIT_RULES, VIEW_RULES, canView } from '$lib/utils/permissions';
import { menuItems } from '$lib/config/menu';

interface AuthState {
	user: User | null;
	token: string | null;
	isAuthenticated: boolean;
}

const { authMock } = vi.hoisted(() => {
	let value: AuthState = { user: null, token: null, isAuthenticated: false };
	const subs = new Set<(v: AuthState) => void>();
	return {
		authMock: {
			subscribe(fn: (v: AuthState) => void) {
				fn(value);
				subs.add(fn);
				return () => subs.delete(fn);
			},
			set(v: AuthState) {
				value = v;
				subs.forEach((fn) => fn(v));
			},
			get() {
				return value;
			}
		}
	};
});

vi.mock('$lib/stores/auth', () => ({
	auth: authMock,
	logout: vi.fn()
}));

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

function captureRedirect(fn: () => unknown): { status?: number; location?: string } | null {
	try {
		fn();
		return null;
	} catch (err) {
		return err as { status?: number; location?: string };
	}
}

function setUser(u: User | null): void {
	authMock.set({ user: u, token: u ? 'tk' : null, isAuthenticated: u !== null });
}

describe('financeiro +layout — guarda Admin-only total (303)', () => {
	beforeEach(() => {
		setUser(null);
	});

	it.each([1, 2, 3, 4])('role %i em /financeiro/* recebe redirect 303 para /dashboard', (role) => {
		setUser(user({ role: role as 1 | 2 | 3 | 4 }));

		const result = captureRedirect(() => load());

		expect(result?.status).toBe(303);
		expect(result?.location).toBe('/dashboard');
	});

	it('usuário nulo também é redirecionado (303 /dashboard)', () => {
		setUser(null);

		const result = captureRedirect(() => load());

		expect(result?.status).toBe(303);
		expect(result?.location).toBe('/dashboard');
	});

	it('Admin (role 0) passa sem redirect', () => {
		setUser(user({ role: 0 }));

		expect(captureRedirect(() => load())).toBeNull();
	});
});

describe('financeiro — regras Admin-only [0] sem helpers novos', () => {
	it('VIEW_RULES.financeiro segue [0]', () => {
		expect(VIEW_RULES.financeiro).toEqual([0]);
	});

	it('EDIT_RULES.financeiro segue [0]', () => {
		expect(EDIT_RULES.financeiro).toEqual([0]);
	});

	it.each([1, 2, 3, 4])('canView(financeiro) nega role %i', (role) => {
		expect(canView(user({ role: role as 1 | 2 | 3 | 4 }), 'financeiro')).toBe(false);
	});

	it('canView(financeiro) permite Admin e nega nulo', () => {
		expect(canView(user({ role: 0 }), 'financeiro')).toBe(true);
		expect(canView(null, 'financeiro')).toBe(false);
	});
});

describe('menu Financeiro — hide total via canView (7 filhos, sem canSee)', () => {
	function item(): NonNullable<ReturnType<typeof menuItems.find>> {
		const encontrado = menuItems.find((m) => m.label === 'Financeiro');
		if (!encontrado) throw new Error('item "Financeiro" ausente do menu');
		return encontrado;
	}

	it('raiz preserva module/icon/path e tem 7 filhos', () => {
		const fin = item();

		expect(fin.module).toBe('financeiro');
		expect(fin.icon).toBe('financeiro');
		expect(fin.path).toBe('/financeiro');
		expect(fin.children?.map((c) => c.path)).toEqual([
			'/financeiro',
			'/financeiro/lancamentos',
			'/financeiro/contas-pagar',
			'/financeiro/contas-receber',
			'/financeiro/doacoes',
			'/financeiro/custeio',
			'/financeiro/relatorios'
		]);
	});

	it('filhos seguem os labels do despacho, sem canSee por item', () => {
		const fin = item();

		expect(fin.children?.map((c) => c.label)).toEqual([
			'Resumo',
			'Lançamentos',
			'Contas a pagar',
			'Contas a receber',
			'Doações & recursos',
			'Custeio',
			'Relatórios'
		]);
		for (const child of fin.children ?? []) {
			expect(child.canSee).toBeUndefined();
		}
	});

	it('item Financeiro invisível a não-Admin via canView (hide total)', () => {
		for (const role of [1, 2, 3, 4] as const) {
			expect(canView(user({ role }), 'financeiro')).toBe(false);
		}
		expect(canView(null, 'financeiro')).toBe(false);
	});

	it('item Financeiro visível ao Admin via canView', () => {
		expect(canView(user({ role: 0 }), 'financeiro')).toBe(true);
	});
});
