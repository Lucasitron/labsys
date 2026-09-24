import type { PageLoad } from './$types';
import { listDoacoes } from '$lib/api/financeiro/doacoes';
import { intParam, strParam } from '$lib/utils/stock-url';
import type { DoacoesResult, TipoDoacao } from '$lib/types/financeiro';

export interface DoacoesFilterState {
	search: string;
	tipo: string;
	periodo: string;
	page: number;
}

export const ssr = false;
export const prerender = false;

const TIPOS_VALIDOS: TipoDoacao[] = ['Doação', 'Projeto'];

function tipoValido(valor: string): string {
	return (TIPOS_VALIDOS as string[]).includes(valor) ? valor : '';
}

export const load: PageLoad = async ({ url, fetch }) => {
	const searchParams = url.searchParams;

	const params: DoacoesFilterState = {
		search: strParam(searchParams, 'search'),
		tipo: tipoValido(strParam(searchParams, 'tipo')),
		periodo: strParam(searchParams, 'periodo'),
		page: intParam(searchParams, 'page', 1)
	};

	let resultado: DoacoesResult | null = null;
	let error: string | null = null;
	try {
		resultado = await listDoacoes(
			{
				search: params.search || undefined,
				tipo: (params.tipo || undefined) as TipoDoacao | undefined,
				periodo: params.periodo || undefined,
				page: params.page
			},
			fetch
		);
	} catch {
		error = 'Não foi possível carregar as doações e recursos';
	}

	return { params, resultado, error };
};
