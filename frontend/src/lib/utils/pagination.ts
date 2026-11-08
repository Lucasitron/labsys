import type { Paginado } from '$lib/types/pagination';

export const PADRAO_TAMANHO_PAGINA = 10;

export function totalDePaginas(totalItems: number, pageSize: number): number {
	if (!Number.isInteger(totalItems) || !Number.isInteger(pageSize) || pageSize <= 0) return 0;
	return Math.max(1, Math.ceil(totalItems / pageSize));
}

// Paginação/filtro client-side sobre a lista completa (precedente: estoque ItemsResult).
export function paginarClient<T>(
	todos: T[],
	page = 1,
	pageSize = PADRAO_TAMANHO_PAGINA
): Paginado<T> {
	const totalItems = todos.length;
	const size = pageSize > 0 ? pageSize : PADRAO_TAMANHO_PAGINA;
	const totalPages = totalDePaginas(totalItems, size);
	const atual = Math.min(Math.max(1, page), totalPages);
	return {
		dados: todos.slice((atual - 1) * size, atual * size),
		paginacao: { page: atual, pageSize: size, totalItems, totalPages }
	};
}
