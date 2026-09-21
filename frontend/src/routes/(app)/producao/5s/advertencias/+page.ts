import type { PageLoad } from './$types';
import { obterPenalidades5S } from '$lib/api/producao/client';
import { PADRAO_TAMANHO_PAGINA, paginaDaUrl, tamanhoDaPaginaDaUrl } from '$lib/api/producao/client';
import type { Paginado, Penalidade5S, PenalidadeStatus, PenalidadeTipo } from '$lib/types/producao';

export const ssr = false;
export const prerender = false;

export interface AdvertenciasParams {
	tipo: PenalidadeTipo[];
	situacao: PenalidadeStatus[];
	page: number;
	pageSize: number;
}

export function lerParametros(url: URL): AdvertenciasParams {
	return {
		tipo: url.searchParams.getAll('tipo') as PenalidadeTipo[],
		situacao: url.searchParams.getAll('situacao') as PenalidadeStatus[],
		page: paginaDaUrl(url, 1),
		pageSize: tamanhoDaPaginaDaUrl(url, PADRAO_TAMANHO_PAGINA)
	};
}

export const load: PageLoad = async ({ url, fetch }) => {
	const params = lerParametros(url);
	try {
		const result = await obterPenalidades5S(
			{
				tipo: params.tipo,
				situacao: params.situacao,
				page: params.page,
				pageSize: params.pageSize
			},
			fetch
		);
		return { params, result, error: null };
	} catch (err) {
		return {
			params,
			result: null,
			error: err instanceof Error ? err.message : 'Não foi possível carregar as penalidades'
		};
	}
};