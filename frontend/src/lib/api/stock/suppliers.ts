import type { CreateSupplierPayload, Fornecedor } from '$lib/types/stock';
import { apiFetch } from '$lib/api/client';

// 🔴 Est-006 (TODO): GET /estoque/fornecedores requer perfil ADMIN ou BOLSISTA no backend.
export async function listarFornecedores(
	fetchFn: typeof fetch = fetch
): Promise<Fornecedor[]> {
	return apiFetch<Fornecedor[]>('/estoque/fornecedores', {}, fetchFn);
}

export async function criarFornecedor(payload: CreateSupplierPayload): Promise<Fornecedor> {
	return apiFetch<Fornecedor>('/estoque/fornecedores', {
		method: 'POST',
		body: JSON.stringify(payload)
	});
}

// Alias de compatibilidade usado pelas páginas (lista ≠ opções).
export const fetchSupplierOptions = listarFornecedores;