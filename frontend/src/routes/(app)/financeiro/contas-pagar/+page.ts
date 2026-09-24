import type { PageLoad } from './$types';
import { listLancamentos } from '$lib/api/financeiro/lancamentos';
import { listCategorias } from '$lib/api/financeiro/categorias';
import { intParam, strParam } from '$lib/utils/stock-url';
import type {
	CategoriaFinanceira,
	LancamentosResult,
	LancamentoStatus
} from '$lib/types/financeiro';

export interface ContasPagarFilterState {
	search: string;
	status: string;
	vencimento: string;
	page: number;
}

export const ssr = false;
export const prerender = false;

const STATUS_VALIDOS: LancamentoStatus[] = ['Pendente', 'Atrasado'];
const VENCIMENTO_VALIDOS = ['vencidas', 'mes', 'proximos-30d'];

function statusValido(valor: string): string {
	return (STATUS_VALIDOS as string[]).includes(valor) ? valor : '';
}

function vencimentoValido(valor: string): string {
	return VENCIMENTO_VALIDOS.includes(valor) ? valor : '';
}

export const load: PageLoad = async ({ url, fetch }) => {
	const searchParams = url.searchParams;

	const params: ContasPagarFilterState = {
		search: strParam(searchParams, 'search'),
		status: statusValido(strParam(searchParams, 'status')),
		vencimento: vencimentoValido(strParam(searchParams, 'vencimento')),
		page: intParam(searchParams, 'page', 1)
	};

	// Vista derivada: nenhum endpoint /contas — só listLancamentos com tipo + status fixos.
	const [principal, categorias] = await Promise.allSettled([
		listLancamentos(
			{
				search: params.search || undefined,
				status: params.status || 'Pendente,Atrasado',
				tipo: 'Saída',
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
				? 'Não foi possível carregar as contas a pagar'
				: (null as string | null),
		categorias: categorias.status === 'fulfilled' ? (categorias.value as CategoriaFinanceira[]) : [],
		categoriasError:
			categorias.status === 'rejected'
				? 'Não foi possível carregar as categorias'
				: (null as string | null)
	};
};
