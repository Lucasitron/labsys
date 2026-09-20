import type { Paginacao, Paginado } from '$lib/types/producao';

export type { Paginacao, Paginado };

export const PADRAO_TAMANHO_PAGINA = 10;

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