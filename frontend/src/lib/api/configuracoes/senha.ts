import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';

const TAMANHO_MINIMO = 8;

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

// TODO contrato 🟡: endpoint assumido — PUT /auth/senha
export async function alterarSenha(
	atual: string,
	nova: string,
	confirmacao: string
): Promise<void> {
	if (nova !== confirmacao) {
		throw new Error('A nova senha e a confirmação não coincidem.');
	}
	if (nova.length < TAMANHO_MINIMO) {
		throw new Error(`A nova senha deve ter pelo menos ${TAMANHO_MINIMO} caracteres.`);
	}

	await apiFetch<void>('/auth/senha', {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify({ senhaAtual: atual, novaSenha: nova })
	});
}