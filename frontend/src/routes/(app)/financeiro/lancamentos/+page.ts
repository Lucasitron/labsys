import type { PageLoad } from './$types';
import { listLancamentos } from '$lib/api/financeiro/lancamentos';
import { listCategorias } from '$lib/api/financeiro/categorias';
import { intParam, sanitizeSort, strParam } from '$lib/utils/stock-url';
import type {
	CategoriaFinanceira,
	LancamentosResult,
	LancamentoStatus,
	TipoLancamento
} from '$lib/types/financeiro';

export interface LancamentosFilterState {
	search: string;
	status: string;
	tipo: string;
	categoria: string;
	periodo: string;
	ordenar: string;
	page: number;
	pageSize: number;
}

export const ssr = false;
export const prerender = false;

const STATUS_VALIDOS: LancamentoStatus[] = ['Pendente', 'Pago', 'Atrasado', 'Cancelado'];
const TIPOS_VALIDOS: TipoLancamento[] = ['Entrada', 'Saída'];
const ORDENAR_VALIDAS = ['recentes', 'vencimento', 'valor-maior', 'valor-menor'];
const PERIODOS_VALIDOS = ['7d', '30d', 'mes', 'ano'];

function statusValido(valor: string): string {
	return (STATUS_VALIDOS as string[]).includes(valor) ? valor : '';
}

function tipoValido(valor: string): string {
	return (TIPOS_VALIDOS as string[]).includes(valor) ? valor : '';
}

export const load: PageLoad = async ({ url, fetch }) => {
	const searchParams = url.searchParams;

	const params: LancamentosFilterState = {
		search: strParam(searchParams, 'search'),
		status: statusValido(strParam(searchParams, 'status')),
		tipo: tipoValido(strParam(searchParams, 'tipo')),
		categoria: strParam(searchParams, 'categoria'),
		periodo: sanitizeSort(strParam(searchParams, 'periodo'), PERIODOS_VALIDOS),
		ordenar: sanitizeSort(strParam(searchParams, 'ordenar'), ORDENAR_VALIDAS),
		page: intParam(searchParams, 'page', 1),
		pageSize: intParam(searchParams, 'pageSize', 10, 10, 100)
	};

	const [principal, categorias] = await Promise.allSettled([
		listLancamentos(
			{
				search: params.search || undefined,
				status: params.status || undefined,
				tipo: (params.tipo || undefined) as TipoLancamento | undefined,
				categoria: params.categoria || undefined,
				periodo: params.periodo || undefined,
				ordenar: params.ordenar || undefined,
				page: params.page,
				pageSize: params.pageSize
			},
			fetch
		),
		listCategorias(fetch)
	]);

	return {
		params,
		resultado: principal.status === 'fulfilled' ? (principal.value as LancamentosResult) : null,
		error:
			principal.status === 'rejected' ? 'Não foi possível carregar os lançamentos' : (null as string | null),
		categorias: categorias.status === 'fulfilled' ? (categorias.value as CategoriaFinanceira[]) : [],
		categoriasError:
			categorias.status === 'rejected'
				? 'Não foi possível carregar as categorias'
				: (null as string | null)
	};
};
