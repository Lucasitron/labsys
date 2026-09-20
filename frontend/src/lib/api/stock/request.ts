import { get } from 'svelte/store';
import { goto } from '$app/navigation';
import { ApiError, API_BASE } from '../client';
import { auth, logout } from '$lib/stores/auth';
import { browser } from '$app/environment';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

function loginRedirectTarget(): string {
	const fallback = '/estoque';
	if (!browser) return fallback;
	const current = window.location.pathname + window.location.search;
	if (current === '/auth/login') return fallback;
	return current;
}

async function handleUnauthorized(err: unknown): Promise<never> {
	if (err instanceof ApiError && err.status === 401) {
		logout();
		await goto(`/auth/login?redirect=${encodeURIComponent(loginRedirectTarget())}`);
	}
	throw err;
}

async function readErrorPayload(response: Response): Promise<{ code?: string; message?: string; error?: string }> {
	try {
		return (await response.json()) as { code?: string; message?: string; error?: string };
	} catch {
		return {};
	}
}

export async function stockFetch<T>(
	path: string,
	init: RequestInit = {},
	fetchFn: typeof fetch = fetch
): Promise<T> {
	return (await stockFetchResponse(path, init, fetchFn)).json() as Promise<T>;
}

export async function stockFetchBlob(
	path: string,
	init: RequestInit = {},
	fetchFn: typeof fetch = fetch
): Promise<Blob> {
	return (await stockFetchResponse(path, init, fetchFn)).blob();
}

export async function stockFetchResponse(
	path: string,
	init: RequestInit = {},
	fetchFn: typeof fetch = fetch
): Promise<Response> {
try {
			const controller = new AbortController();
			const timeout = setTimeout(() => controller.abort(), 15_000);

			const headers = new Headers(init.headers ?? {});
			for (const [key, value] of Object.entries(bearer())) {
				if (!headers.has(key)) headers.set(key, value);
			}
			if (!headers.has('Content-Type')) {
				headers.set('Content-Type', 'application/json');
			}

			let response: Response;
			try {
				response = await fetchFn(`${API_BASE}${path}`, {
					...init,
					headers,
					signal: controller.signal
				});
			} finally {
				clearTimeout(timeout);
			}

		if (!response.ok) {
			const payload = await readErrorPayload(response);
			const code = payload.code ?? payload.error ?? 'UNKNOWN';
			throw new ApiError(
				response.status,
				code,
				payload.message ?? `Erro ${response.status}`
			);
		}

		return response;
	} catch (err) {
		return handleUnauthorized(err);
	}
}

export function buildQuery(
	params: Record<string, string | number | undefined | string[]>
): string {
	const search = new URLSearchParams();
	for (const [key, value] of Object.entries(params)) {
		if (value === undefined) continue;
		if (Array.isArray(value)) {
			for (const v of value) {
				if (v) search.append(key, v);
			}
		} else if (String(value) !== '') {
			search.append(key, String(value));
		}
	}
	const qs = search.toString();
	return qs ? `?${qs}` : '';
}