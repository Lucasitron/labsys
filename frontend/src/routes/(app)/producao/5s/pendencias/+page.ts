import type { PageLoad } from './$types';
import { obterPendencias5S } from '$lib/api/producao/client';
import { PADRAO_TAMANHO_PAGINA, paginaDaUrl, tamanhoDaPaginaDaUrl } from '$lib/api/producao/client';
import type { GravidadePendencia, Paginado, Pendencia5S, SituacaoPendencia } from '$lib/types/producao';

export const ssr = false;
export const prerender = false;

export interface PendenciasParams {
	gravidade: GravidadePendencia[];
	setor: string[];
	situacao: SituacaoPendencia[];
	page: number;
	pageSize: number;
}

export function lerParametros(url: URL): PendenciasParams {
	return {
		gravidade: url.searchParams.getAll('gravidade') as GravidadePendencia[],
		setor: url.searchParams.getAll('setor'),
		situacao: url.searchParams.getAll('situacao') as SituacaoPendencia[],
		page: paginaDaUrl(url, 1),
		pageSize: tamanhoDaPaginaDaUrl(url, PADRAO_TAMANHO_PAGINA)
	};
}

export const load: PageLoad = async ({ url, fetch }) => {
	const params = lerParametros(url);
	try {
		const result = await obterPendencias5S(
			{
				gravidade: params.gravidade,
				setor: params.setor,
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
			error: err instanceof Error ? err.message : 'Não foi possível carregar as pendências'
		};
	}
};