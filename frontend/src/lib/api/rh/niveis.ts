import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type { ConvidarUsuarioPayload, Nivel, NivelMatrix, NivelMembro } from '$lib/types/rh';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function getNiveis(fetchFn: typeof fetch = fetch): Promise<NivelMatrix> {
	return await apiFetch<NivelMatrix>('/api/rh/niveis', { headers: bearer() }, fetchFn);
}

export async function alterarNivel(
	membroId: string,
	nivel: Nivel,
	fetchFn: typeof fetch = fetch
): Promise<NivelMembro> {
	return await apiFetch<NivelMembro>(
		`/api/rh/niveis/${membroId}/membros`,
		{ method: 'PATCH', headers: bearer(), body: JSON.stringify({ nivel }) },
		fetchFn
	);
}

export async function convidarUsuario(
	payload: ConvidarUsuarioPayload,
	fetchFn: typeof fetch = fetch
): Promise<NivelMembro> {
	return await apiFetch<NivelMembro>(
		'/api/rh/niveis/convites',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
}
