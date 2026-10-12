import { get } from 'svelte/store';
import { auth } from '$lib/stores/auth';

// Fonte única do header de autenticação para as chamadas de API.
export function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}
