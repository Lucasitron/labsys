import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type { CategoriaFinanceira, CreateCategoriaPayload } from '$lib/types/financeiro';

const BASE = '/api/financeiro/categorias';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function listCategorias(fetchFn: typeof fetch = fetch): Promise<CategoriaFinanceira[]> {
	return await apiFetch<CategoriaFinanceira[]>(BASE, { headers: bearer() }, fetchFn);
}

export async function createCategoria(
	payload: CreateCategoriaPayload,
	fetchFn: typeof fetch = fetch
): Promise<CategoriaFinanceira> {
	return await apiFetch<CategoriaFinanceira>(
		BASE,
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}
