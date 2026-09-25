import type { PageLoad } from './$types';
import { obterInspecoes5S } from '$lib/api/producao/client';
import { PADRAO_TAMANHO_PAGINA, paginaDaUrl, tamanhoDaPaginaDaUrl } from '$lib/api/producao/client';
import type { Inspecao5S, InspecaoStatus, Paginado } from '$lib/types/producao';

export const ssr = false;
export const prerender = false;

export interface AuditoriaParams {
	status: InspecaoStatus[];
	setor: string[];
	page: number;
	pageSize: number;
}

export function lerParametros(url: URL): AuditoriaParams {
	return {
		status: url.searchParams.getAll('status') as InspecaoStatus[],
		setor: url.searchParams.getAll('setor'),
		page: paginaDaUrl(url, 1),
		pageSize: tamanhoDaPaginaDaUrl(url, PADRAO_TAMANHO_PAGINA)
	};
}

export const load: PageLoad = async ({ url, fetch }) => {
	const params = lerParametros(url);
	try {
		const result = await obterInspecoes5S(
			{
				status: params.status,
				setor: params.setor,
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
			error: err instanceof Error ? err.message : 'Não foi possível carregar as auditorias'
		};
	}
};