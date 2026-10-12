import { apiFetch, apiFetchBlob } from '../client';
import { bearer } from '$lib/api/bearer';

function withAuth(init: RequestInit): RequestInit {
	const headers = new Headers(init.headers);
	for (const [key, value] of Object.entries(bearer())) {
		if (!headers.has(key)) headers.set(key, value);
	}
	return { ...init, headers };
}

// Wrappers do Estoque sobre o cliente HTTP único (apiFetch): mesma
// política de timeout/401/refresh. `FormData` é tratado no próprio apiFetch.
export function stockFetch<T>(
	path: string,
	init: RequestInit = {},
	fetchFn: typeof fetch = fetch
): Promise<T> {
	return apiFetch<T>(path, withAuth(init), fetchFn);
}

export function stockFetchBlob(
	path: string,
	init: RequestInit = {},
	fetchFn: typeof fetch = fetch
): Promise<Blob> {
	return apiFetchBlob(path, withAuth(init), fetchFn);
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
