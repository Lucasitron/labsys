import { get, writable } from 'svelte/store';
import { browser } from '$app/environment';
import { login as apiLogin } from '$lib/api/auth';
import type { Module, Role, User } from '$lib/types/auth';

const STORAGE_KEY = 'fablab.auth';

export interface AuthState {
	user: User | null;
	token: string | null;
	isAuthenticated: boolean;
}

interface PersistedAuth {
	user: User;
	token: string;
}

const EMPTY: AuthState = { user: null, token: null, isAuthenticated: false };

const VIEW_RULES: Record<Module, readonly Role[]> = {
	dashboard: [0, 1, 2, 3, 4],
	rh: [0, 1, 2, 3, 4],
	estoque: [0, 1, 2, 3],
	vendas: [0, 1, 2, 3],
	financeiro: [0],
	producao: [0, 1, 2, 3]
};

const EDIT_RULES: Record<Module, readonly Role[]> = {
	dashboard: [],
	rh: [0, 1, 2],
	estoque: [0],
	vendas: [0],
	financeiro: [0],
	producao: [0]
};

function restore(): AuthState {
	if (!browser) return EMPTY;

	try {
		const raw = localStorage.getItem(STORAGE_KEY);
		if (raw) {
			const stored = JSON.parse(raw) as PersistedAuth;
			if (stored?.user && stored?.token) {
				return { user: stored.user, token: stored.token, isAuthenticated: true };
			}
		}
	} catch {
		// armazenamento indisponível
	}

	return EMPTY;
}

export const auth = writable<AuthState>(restore());

function persist(state: AuthState): void {
	if (!browser || !state.user || !state.token) return;

	try {
		const stored: PersistedAuth = { user: state.user, token: state.token };
		localStorage.setItem(STORAGE_KEY, JSON.stringify(stored));
	} catch {
		// armazenamento indisponível
	}
}

export async function login(username: string, password: string): Promise<void> {
	const response = await apiLogin(username, password);
	const state: AuthState = {
		user: response.user,
		token: response.token,
		isAuthenticated: true
	};

	auth.set(state);
	persist(state);
}

export function logout(): void {
	auth.set(EMPTY);

	if (browser) {
		try {
			localStorage.removeItem(STORAGE_KEY);
		} catch {
			// armazenamento indisponível
		}
	}
}

export function canView(module: Module): boolean {
	const { user } = get(auth);
	return user !== null && VIEW_RULES[module].includes(user.role);
}

export function canEdit(module: Module): boolean {
	const { user } = get(auth);
	return user !== null && EDIT_RULES[module].includes(user.role);
}