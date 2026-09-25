import type { PageLoad } from './$types';
import { listClientes } from '$lib/api/vendas/clientes';
import { intParam, sanitizeSort, strParam } from '$lib/utils/stock-url';
import type { ClientesResult, TipoPessoa } from '$lib/types/vendas';

export interface ClientesFilterState {
	search: string;
	tipo: string;
	tags: string;
	ordenar: string;
	page: number;
	pageSize: number;
}

export const ssr = false;
export const prerender = false;

const ORDENAR_VALIDAS = ['nome', 'recentes', 'compras'];

function tipoValido(valor: string): TipoPessoa | undefined {
	return valor === 'pf' || valor === 'pj' ? valor : undefined;
}

export const load: PageLoad = async ({ url, fetch }) => {
	const searchParams = url.searchParams;

	const params: ClientesFilterState = {
		search: strParam(searchParams, 'search'),
		tipo: strParam(searchParams, 'tipo'),
		tags: strParam(searchParams, 'tags'),
		ordenar: sanitizeSort(strParam(searchParams, 'ordenar'), ORDENAR_VALIDAS),
		page: intParam(searchParams, 'page', 1),
		pageSize: intParam(searchParams, 'pageSize', 10, 10, 100)
	};

	try {
		const resultado: ClientesResult = await listClientes(
			{
				search: params.search || undefined,
				tipo: tipoValido(params.tipo),
				tags: params.tags || undefined,
				ordenar: params.ordenar || undefined,
				page: params.page,
				pageSize: params.pageSize
			},
			fetch
		);
		return { params, resultado, error: null as string | null };
	} catch {
		return {
			params,
			resultado: null,
			error: 'Não foi possível carregar os clientes'
		};
	}
};
