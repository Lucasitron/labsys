import { get } from 'svelte/store';
import { auth } from '$lib/stores/auth';
import type { Paginacao, Paginado } from '$lib/types/producao';

export type { Paginacao, Paginado };

export const PADRAO_TAMANHO_PAGINA = 10;

export function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

type QueryValue = string | number | undefined | string[];

export function montarQuery(params: Record<string, QueryValue>): string {
	const search = new URLSearchParams();
	for (const [chave, valor] of Object.entries(params)) {
		if (valor === undefined) continue;
		if (Array.isArray(valor)) {
			for (const v of valor) {
				if (v !== '') search.append(chave, v);
			}
		} else if (valor !== '') {
			search.append(chave, String(valor));
		}
	}
	const qs = search.toString();
	return qs ? `?${qs}` : '';
}

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

export function filtrosDaUrl(url: URL, chaves: readonly string[]): Record<string, string[]> {
	const filtros: Record<string, string[]> = {};
	for (const chave of chaves) {
		const valores = url.searchParams.getAll(chave);
		if (valores.length > 0) filtros[chave] = valores;
	}
	return filtros;
}

export function totalDePaginas(totalItems: number, pageSize: number): number {
	if (!Number.isInteger(totalItems) || !Number.isInteger(pageSize) || pageSize <= 0) return 0;
	return Math.max(1, Math.ceil(totalItems / pageSize));
}

// Back serve listas como `{data:[...]}` (ResourceCollection) — desembrulha p/ array.
export function unwrapLista<T>(res: T[] | { data: T[] } | null | undefined): T[] {
	if (Array.isArray(res)) return res;
	if (res && Array.isArray((res as { data?: T[] }).data)) {
		return (res as { data: T[] }).data;
	}
	return [];
}

// Paginação/filtro client-side sobre a lista completa (precedente: estoque ItemsResult).
export function paginarClient<T>(todos: T[], page = 1, pageSize = PADRAO_TAMANHO_PAGINA): Paginado<T> {
	const totalItems = todos.length;
	const size = pageSize > 0 ? pageSize : PADRAO_TAMANHO_PAGINA;
	const totalPages = totalDePaginas(totalItems, size);
	const atual = Math.min(Math.max(1, page), totalPages);
	return {
		dados: todos.slice((atual - 1) * size, atual * size),
		paginacao: { page: atual, pageSize: size, totalItems, totalPages }
	};
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

export const KANBAN_UPPER: Record<string, string> = {
	FILA: 'Fila',
	PRODUCAO: 'Produção',
	ACABAMENTO: 'Acabamento',
	PRONTO: 'Pronto',
	ENTREGUE: 'Entregue'
};

export const MAQUINA_STATUS_UPPER: Record<string, string> = {
	DISPONIVEL: 'Disponível',
	EM_USO: 'Em uso',
	MANUTENCAO: 'Em manutenção',
	SUSPENSA: 'Suspensa'
};

export function paginadoVazio<T>(): Paginado<T> {
	return {
		dados: [],
		paginacao: {
			page: 1,
			pageSize: PADRAO_TAMANHO_PAGINA,
			totalItems: 0,
			totalPages: totalDePaginas(0, PADRAO_TAMANHO_PAGINA)
		}
	};
}

export function paginaSeguinte<T>(paginas: Paginado<T>): number | null {
	return paginas.paginacao.page < paginas.paginacao.totalPages ? paginas.paginacao.page + 1 : null;
}

export function paginaAnterior<T>(paginas: Paginado<T>): number | null {
	return paginas.paginacao.page > 1 ? paginas.paginacao.page - 1 : null;
}

export function mostrarDeAte<T>(paginas: Paginado<T>): string {
	const { page, pageSize, totalItems } = paginas.paginacao;
	if (totalItems === 0) return '0';
	const inicio = (page - 1) * pageSize + 1;
	const fim = Math.min(page * pageSize, totalItems);
	return `${inicio}–${fim} de ${totalItems}`;
}

export * from './resumo';
export * from './projetos';
export * from './tarefas';
export * from './kanban';
export * from './maquinas';
export * from './setores-5s';
export * from './inspecoes-5s';
export * from './pendencias-5s';
export * from './ranking-5s';
export * from './penalidades-5s';
export * from './mesas';
export * from './parametros-5s';