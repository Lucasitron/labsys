import { get } from 'svelte/store';
import { apiFetch, ApiError, NetworkError } from '$lib/api/client';
import { fetchMe } from '$lib/api/auth';
import { auth } from '$lib/stores/auth';
import type { Perfil } from '$lib/types/configuracoes';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

// TODO contrato 🟡: endpoint assumido — GET /configuracoes/perfil
export async function getPerfil(fetchFn: typeof fetch = fetch): Promise<Perfil> {
	try {
		return await apiFetch<Perfil>('/configuracoes/perfil', { headers: bearer() }, fetchFn);
	} catch (err) {
		const ehRede = err instanceof NetworkError;
		const eh5xx = err instanceof ApiError && err.status >= 500;
		if (!ehRede && !eh5xx) throw err;
		const usuario = await fetchMe(fetchFn);
		return {
			nome: usuario.name,
			email: usuario.email,
			funcao: null,
			avatar: null
		};
	}
}

// TODO contrato 🟡: endpoint assumido — PUT /configuracoes/perfil
export function updatePerfil(payload: Perfil): Promise<Perfil> {
	return apiFetch<Perfil>('/configuracoes/perfil', {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}