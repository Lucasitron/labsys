import type { CreateEmprestimoPayload, Emprestimo } from '$lib/types/stock';
import { stockFetch } from './request';

function mapEmprestimo(raw: Emprestimo): Emprestimo {
	return { ...raw, quantidade: Number(raw.quantidade) };
}

export async function listarAtrasados(
	fetchFn: typeof fetch = fetch
): Promise<Emprestimo[]> {
	const raw = await stockFetch<Emprestimo[]>('/estoque/emprestimos/atrasados', {}, fetchFn);
	return raw.map(mapEmprestimo);
}

export async function buscarEmprestimo(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<Emprestimo> {
	return mapEmprestimo(await stockFetch<Emprestimo>(`/estoque/emprestimos/${id}`, {}, fetchFn));
}

export async function criarEmprestimo(payload: CreateEmprestimoPayload): Promise<Emprestimo> {
	return mapEmprestimo(await stockFetch<Emprestimo>('/estoque/emprestimos', {
		method: 'POST',
		body: JSON.stringify(payload)
	}));
}

export async function devolverEmprestimo(id: string): Promise<Emprestimo> {
	return mapEmprestimo(await stockFetch<Emprestimo>(`/estoque/emprestimos/${id}/devolucao`, {
		method: 'PUT'
	}));
}
