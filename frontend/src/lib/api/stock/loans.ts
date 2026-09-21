import type { CreateEmprestimoPayload, Emprestimo } from '$lib/types/stock';
import { stockFetch } from './request';

export async function listarAtrasados(
	fetchFn: typeof fetch = fetch
): Promise<Emprestimo[]> {
	return stockFetch<Emprestimo[]>('/estoque/emprestimos/atrasados', {}, fetchFn);
}

export async function buscarEmprestimo(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<Emprestimo> {
	return stockFetch<Emprestimo>(`/estoque/emprestimos/${id}`, {}, fetchFn);
}

export async function criarEmprestimo(payload: CreateEmprestimoPayload): Promise<Emprestimo> {
	return stockFetch<Emprestimo>('/estoque/emprestimos', {
		method: 'POST',
		body: JSON.stringify(payload)
	});
}

export async function devolverEmprestimo(id: string): Promise<Emprestimo> {
	return stockFetch<Emprestimo>(`/estoque/emprestimos/${id}/devolucao`, {
		method: 'PUT',
		body: JSON.stringify({})
	});
}