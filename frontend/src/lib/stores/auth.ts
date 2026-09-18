import { get, writable } from 'svelte/store';
import { browser } from '$app/environment';
import { login as apiLogin } from '$lib/api/auth';
import type { User } from '$lib/types/auth';

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

export function setUser(user: User): void {
	auth.update((state) => {
		const next: AuthState = { ...state, user };
		persist(next);
		return next;
	});
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