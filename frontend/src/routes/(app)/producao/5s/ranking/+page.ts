import type { PageLoad } from './$types';
import { obterEvolucaoRanking5S, obterRanking5S } from '$lib/api/producao/client';
import type { EvolucaoRanking5S, ResultadoRanking5S } from '$lib/api/producao/ranking-5s';

export const ssr = false;
export const prerender = false;

export interface RankingParams {
	mes: string;
}

export function lerParametros(url: URL): RankingParams {
	return {
		mes: url.searchParams.get('mes') ?? new Date().toISOString().slice(0, 7)
	};
}

export const load: PageLoad = async ({ url, fetch }) => {
	const params = lerParametros(url);
	let result: ResultadoRanking5S | null = null;
	let erroRanking: string | null = null;
	let evolucao: EvolucaoRanking5S[] | null = null;
	let erroEvolucao: string | null = null;

	try {
		result = await obterRanking5S({ mes: params.mes }, fetch);
	} catch (err) {
		erroRanking = err instanceof Error ? err.message : 'Não foi possível carregar o ranking';
	}

	try {
		evolucao = await obterEvolucaoRanking5S({ mes: params.mes }, fetch);
	} catch (err) {
		erroEvolucao = err instanceof Error ? err.message : 'Não foi possível carregar a evolução';
	}

	return { params, result, erroRanking, evolucao, erroEvolucao };
};