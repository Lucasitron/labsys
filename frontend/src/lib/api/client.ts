import { goto } from '$app/navigation';
import { browser } from '$app/environment';

export const API_BASE: string =
	import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

const TIMEOUT_MS = 15_000;

const AUTH_PUBLIC_PATHS = new Set(['/auth/login', '/auth/refresh']);

	let redirecting = false;

// Refresh compartilhado entre 401s concorrentes (1 retry com o refreshToken).
let refreshInFlight: Promise<string | null> | null = null;

export class ApiError extends Error {
	readonly status: number;
	readonly code: string;

	constructor(status: number, code: string, message: string) {
		super(message);
		this.name = 'ApiError';
		this.status = status;
		this.code = code;
	}
}

export class NetworkError extends Error {
	constructor(message = 'Falha de conexão. Verifique sua rede.') {
		super(message);
		this.name = 'NetworkError';
	}
}

interface ErrorPayload {
	error?: string;
	code?: string;
	message?: string;
}

export function sanitizeRedirect(path: string | null): string {
	if (!path || !path.startsWith('/') || path.startsWith('//')) return '/dashboard';
	if (/[:\\\u0000-\u001f]/.test(path)) return '/dashboard';
	return path;
}

function isPublicAuthPath(path: string): boolean {
	return AUTH_PUBLIC_PATHS.has(path);
}

function currentPath(): string {
	return browser ? window.location.pathname : '/';
}

async function handleUnauthorized(requestPath: string): Promise<void> {
	if (isPublicAuthPath(requestPath) || redirecting || currentPath() === '/auth/login') return;

	redirecting = true;
	try {
		const { logout } = await import('$lib/stores/auth');
		logout();
		const redirect = encodeURIComponent(sanitizeRedirect(currentPath()));
		await goto(`/auth/login?redirect=${redirect}`);
	} finally {
		redirecting = false;
	}
}

export async function apiFetch<T>(
	path: string,
	init: RequestInit = {},
	fetchFn: typeof fetch = fetch,
	retried = false
): Promise<T> {
	const controller = new AbortController();
	const timeout = setTimeout(() => controller.abort(), TIMEOUT_MS);

	const headers = new Headers(init.headers);
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
	} catch (err) {
		if (err instanceof DOMException && err.name === 'AbortError') {
			throw new NetworkError('Tempo limite excedido. Verifique sua rede.');
		}
		throw new NetworkError();
	} finally {
		clearTimeout(timeout);
	}

	if (!response.ok) {
		if (response.status === 401 && !retried && !isPublicAuthPath(path)) {
			refreshInFlight ??= tryRefresh(fetchFn);
			const renewed = await refreshInFlight;
			refreshInFlight = null;
			if (renewed) {
				const retryInit: RequestInit = { ...init, headers: withRefreshedAuth(init, renewed) };
				return apiFetch<T>(path, retryInit, fetchFn, true);
			}
		}

		const payload = await readPayload(response);
		const code = payload.code ?? payload.error ?? 'UNKNOWN';
		const apiError = new ApiError(response.status, code, payload.message ?? `Erro ${response.status}`);

		if (response.status === 401) {
			await handleUnauthorized(path);
		}

		throw apiError;
	}

	if (response.status === 204) return undefined as T;
	const text = await response.text();
	return (text ? JSON.parse(text) : undefined) as T;
}

async function readPayload(response: Response): Promise<ErrorPayload> {
	try {
		return (await response.json()) as ErrorPayload;
	} catch {
		return {};
	}
}

// T01: tenta 1x o refresh com o refreshToken persistido (sem Bearer) antes do logout.
// Leitura síncrona do persistido (mesma chave da store) p/ não escalonar 401s
// concorrentes — a flag anti-duplicidade do logout continua valendo.
function readPersistedRefreshToken(): string | null {
	if (!browser) return null;
	try {
		const raw = localStorage.getItem('fablab.auth');
		if (!raw) return null;
		const data = JSON.parse(raw) as { refreshToken?: string | null };
		return data.refreshToken ?? null;
	} catch {
		return null;
	}
}

async function tryRefresh(fetchFn: typeof fetch): Promise<string | null> {
	const refreshToken = readPersistedRefreshToken();
	if (!refreshToken) return null;
	try {
		const res = await fetchFn(`${API_BASE}/auth/refresh`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ refreshToken })
		});
		if (!res.ok) return null;
		const data = (await res.json()) as { accessToken?: string; refreshToken?: string | null };
		if (!data.accessToken) return null;
		const { applyRefreshedTokens } = await import('$lib/stores/auth');
		applyRefreshedTokens(data.accessToken, data.refreshToken ?? null);
		return data.accessToken;
	} catch {
		return null;
	}
}

function withRefreshedAuth(init: RequestInit, token: string): Headers {
	const headers = new Headers(init.headers);
	if (headers.has('Authorization')) headers.set('Authorization', `Bearer ${token}`);
	return headers;
}