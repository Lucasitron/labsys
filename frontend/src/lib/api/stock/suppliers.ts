import type { CreateSupplierPayload, Fornecedor } from '$lib/types/stock';
import { stockFetch } from './request';

// 🔴 Est-006 (TODO): GET /estoque/fornecedores requer perfil ADMIN ou BOLSISTA no backend.
export async function listarFornecedores(
	fetchFn: typeof fetch = fetch
): Promise<Fornecedor[]> {
	return stockFetch<Fornecedor[]>('/estoque/fornecedores', {}, fetchFn);
}

export async function criarFornecedor(payload: CreateSupplierPayload): Promise<Fornecedor> {
	return stockFetch<Fornecedor>('/estoque/fornecedores', {
		method: 'POST',
		body: JSON.stringify(payload)
	});
}

// Aliases legados (páginas ainda importam `fetchSupplierOptions`).
export const listarFornecedoresOptions = listarFornecedores;
export const fetchSupplierOptions = listarFornecedores;