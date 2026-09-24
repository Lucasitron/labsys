import type { PageLoad } from './$types';
import { listFechamentos, listValoresHora } from '$lib/api/financeiro/custeio';
import { listSolicitacoesCompra } from '$lib/api/financeiro/solicitacoes-compra';
import type {
	CusteioTab,
	FechamentoEncomenda,
	SolicitacaoCompra,
	ValorHoraNivel
} from '$lib/types/financeiro';

export const ssr = false;
export const prerender = false;

const TABS_VALIDAS: CusteioTab[] = ['fechamentos', 'custos', 'valores', 'compras'];

export interface CusteioState {
	tab: CusteioTab;
}

export const load: PageLoad = async ({ url, fetch }) => {
	const bruta = (url.searchParams.get('tab') ?? '').trim();
	const tab: CusteioTab = (TABS_VALIDAS as string[]).includes(bruta)
		? (bruta as CusteioTab)
		: 'fechamentos';

	const [fechamentos, valores, compras] = await Promise.allSettled([
		listFechamentos(fetch),
		listValoresHora(fetch),
		listSolicitacoesCompra(fetch)
	]);

	return {
		tab,
		fechamentos:
			fechamentos.status === 'fulfilled' ? (fechamentos.value as FechamentoEncomenda[]) : [],
		fechamentosError:
			fechamentos.status === 'rejected'
				? 'Não foi possível carregar os fechamentos'
				: (null as string | null),
		valoresHora: valores.status === 'fulfilled' ? (valores.value as ValorHoraNivel[]) : [],
		valoresError:
			valores.status === 'rejected'
				? 'Não foi possível carregar os valores/hora'
				: (null as string | null),
		compras: compras.status === 'fulfilled' ? (compras.value as SolicitacaoCompra[]) : [],
		comprasError:
			compras.status === 'rejected'
				? 'Não foi possível carregar as solicitações de compra'
				: (null as string | null)
	};
};
