import type { PageLoad } from './$types';
import { obterSetores5S } from '$lib/api/producao/client';
import type { Cartao5S, Paginado, Setor5S } from '$lib/types/producao';
import { PADRAO_TAMANHO_PAGINA } from '$lib/api/producao/client';
import { paginaDaUrl, tamanhoDaPaginaDaUrl } from '$lib/api/producao/client';

export const ssr = false;
export const prerender = false;

export interface SetoresParams {
	search: string;
	cartao: string[];
	responsavel: string[];
	page: number;
	pageSize: number;
}

function lerParametros(url: URL): SetoresParams {
	return {
		search: url.searchParams.get('search') ?? '',
		cartao: url.searchParams.getAll('cartao'),
		responsavel: url.searchParams.getAll('responsavel'),
		page: paginaDaUrl(url, 1),
		pageSize: tamanhoDaPaginaDaUrl(url, PADRAO_TAMANHO_PAGINA)
	};
}

export const load: PageLoad = async ({ url, fetch }) => {
	const params = lerParametros(url);

	try {
		const result = await obterSetores5S(
			{
				page: params.page,
				pageSize: params.pageSize
			},
			fetch
		);
		return { params, result, error: null as string | null };
	} catch (err) {
		return {
			params,
			result: null as Paginado<Setor5S> | null,
			error: err instanceof Error ? err.message : 'Não foi possível carregar os setores'
		};
	}
};

const CARTOES: Cartao5S[] = ['Verde', 'Amarelo', 'Vermelho'];
export { CARTOES };