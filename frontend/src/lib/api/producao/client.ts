import type { Paginacao, Paginado } from '$lib/types/producao';
import { PADRAO_TAMANHO_PAGINA, paginarClient, totalDePaginas } from '$lib/utils/pagination';


function lerNumero(url: URL, chave: string, fallback: number, max: number): number {
	const valor = Number(url.searchParams.get(chave));
	if (!Number.isInteger(valor) || valor < 1) return fallback;
	return Math.min(max, valor);
}

export function paginaDaUrl(url: URL, fallback = 1): number {
	return lerNumero(url, 'page', fallback, 1_000_000);
}

export function tamanhoDaPaginaDaUrl(
	url: URL,
	fallback = PADRAO_TAMANHO_PAGINA,
	max = 100
): number {
	return lerNumero(url, 'pageSize', fallback, max);
}

// Back serve listas como `{data:[...]}` (ResourceCollection) — desembrulha p/ array.

export function unwrapLista<T>(res: T[] | { data: T[] } | null | undefined): T[] {
	if (Array.isArray(res)) return res;
	if (res && Array.isArray((res as { data?: T[] }).data)) {
		return (res as { data: T[] }).data;
	}
	return [];
}

// O back pode devolver Paginado (dados), `{data}` ou array puro.
export function paraPaginado<T>(
	res: Paginado<T> | T[] | { data: T[] } | null | undefined,
	page = 1,
	pageSize = PADRAO_TAMANHO_PAGINA
): Paginado<T> {
	if (res && !Array.isArray(res) && Array.isArray((res as Paginado<T>).dados)) {
		return res as Paginado<T>;
	}
	return paginarClient(unwrapLista(res as T[] | { data: T[] }), page, pageSize);
}

// Mapper UPPER (back) → Title (exibição/filtro).
export const STATUS_PROJETO_UPPER: Record<string, string> = {
	PLANEJADO: 'Planejado',
	EM_ANDAMENTO: 'Em andamento',
	CONCLUIDO: 'Concluído',
	CANCELADO: 'Cancelado'
};

export const STATUS_TAREFA_UPPER: Record<string, string> = {
	PENDENTE: 'Pendente',
	EM_ANDAMENTO: 'Em andamento',
	CONCLUIDA: 'Concluída',
	ATRASADA: 'Atrasada',
	BLOQUEADA: 'Bloqueada'
};

export const PRIORIDADE_UPPER: Record<string, string> = {
	BAIXA: 'Baixa',
	MEDIA: 'Média',
	ALTA: 'Alta'
};

export const MAQUINA_STATUS_UPPER: Record<string, string> = {
	DISPONIVEL: 'Disponível',
	EM_USO: 'Em uso',
	MANUTENCAO: 'Em manutenção'
};

