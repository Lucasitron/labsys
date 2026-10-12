import { bearer } from '$lib/api/bearer';
import { apiFetch } from '$lib/api/client';

const TAMANHO_MINIMO = 8;


// Contrato real (Laravel): PUT /auth/senha (Bearer)
// body { senhaAtual, novaSenha, confirmacaoSenha } → { mensagem }.
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
		body: JSON.stringify({ senhaAtual: atual, novaSenha: nova, confirmacaoSenha: confirmacao })
	});
}