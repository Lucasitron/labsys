import type { PageLoad } from './$types';
import { listLancamentos } from '$lib/api/financeiro/lancamentos';
import { listCategorias } from '$lib/api/financeiro/categorias';
import { intParam, strParam } from '$lib/utils/stock-url';
import type {
	CategoriaFinanceira,
	LancamentosResult,
	LancamentoStatus
} from '$lib/types/financeiro';

export interface ContasReceberFilterState {
	search: string;
	status: string;
	origem: string;
	page: number;
}

export const ssr = false;
export const prerender = false;

const STATUS_VALIDOS: LancamentoStatus[] = ['Pendente', 'Atrasado'];
const ORIGEM_VALIDAS = ['Encomendas', 'Marketplace', 'Outros'];

function statusValido(valor: string): string {
	return (STATUS_VALIDOS as string[]).includes(valor) ? valor : '';
}

function origemValida(valor: string): string {
	return ORIGEM_VALIDAS.includes(valor) ? valor : '';
}

export const load: PageLoad = async ({ url, fetch }) => {
	const searchParams = url.searchParams;

	const params: ContasReceberFilterState = {
		search: strParam(searchParams, 'search'),
		status: statusValido(strParam(searchParams, 'status')),
		origem: origemValida(strParam(searchParams, 'origem')),
		page: intParam(searchParams, 'page', 1)
	};

	// Vista derivada: nenhum endpoint /contas — só listLancamentos com tipo + status fixos.
	const [principal, categorias] = await Promise.allSettled([
		listLancamentos(
			{
				search: params.search || undefined,
				status: params.status || 'Pendente,Atrasado',
				tipo: 'Entrada',
				page: params.page,
				pageSize: 10
			},
			fetch
		),
		listCategorias(fetch)
	]);

	return {
		params,
		resultado: principal.status === 'fulfilled' ? (principal.value as LancamentosResult) : null,
		error:
			principal.status === 'rejected'
				? 'Não foi possível carregar as contas a receber'
				: (null as string | null),
		categorias: categorias.status === 'fulfilled' ? (categorias.value as CategoriaFinanceira[]) : [],
		categoriasError:
			categorias.status === 'rejected'
				? 'Não foi possível carregar as categorias'
				: (null as string | null)
	};
};
