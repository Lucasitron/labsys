import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { User } from '$lib/types/auth';
import { load } from '../../src/routes/(app)/notificacoes/+layout';
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

function path(url: string): { url: URL } {
	return { url: new URL(url, 'http://localhost') };
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

describe('notificacoes +layout — guarda Admin (303)', () => {
	beforeEach(() => {
		setUser(null);
	});

	it('não-Admin em /notificacoes/preferencias recebe redirect 303 para /notificacoes', () => {
		setUser(user({ role: 2 }));

		const result = captureRedirect(() => load(path('/notificacoes/preferencias')));

		expect(result?.status).toBe(303);
		expect(result?.location).toBe('/notificacoes');
	});

	it('não-Admin em /notificacoes/historico recebe redirect 303 para /notificacoes', () => {
		setUser(user({ role: 1 }));

		const result = captureRedirect(() => load(path('/notificacoes/historico')));

		expect(result?.status).toBe(303);
		expect(result?.location).toBe('/notificacoes');
	});

	it('Admin passa em preferencias e historico sem redirect', () => {
		setUser(user({ role: 0 }));

		expect(captureRedirect(() => load(path('/notificacoes/preferencias')))).toBeNull();
		expect(captureRedirect(() => load(path('/notificacoes/historico')))).toBeNull();
	});

	it('rota /notificacoes passa para qualquer autenticado', () => {
		setUser(user({ role: 3 }));

		expect(captureRedirect(() => load(path('/notificacoes')))).toBeNull();
	});
});

describe('menu Notificações — RBAC hide dos filhos Admin-only', () => {
	function visibleChildren(u: User | null): string[] {
		const item = menuItems.find((m) => m.label === 'Notificações');
		if (!item) throw new Error('item "Notificações" ausente do menu');
		return (item.children ?? [])
			.filter((child) => !child.canSee || child.canSee(u))
			.map((child) => child.path);
	}

	it('não-Admin vê apenas "Minhas notificações"', () => {
		expect(visibleChildren(user({ role: 2 }))).toEqual(['/notificacoes']);
	});

	it('Admin vê também preferências e histórico', () => {
		expect(visibleChildren(user({ role: 0 }))).toEqual([
			'/notificacoes',
			'/notificacoes/preferencias',
			'/notificacoes/historico'
		]);
	});
});
