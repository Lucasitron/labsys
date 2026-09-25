import type { RankingItem, SetorRef } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, montarQuery } from './client';

const BASE = '/producao/ranking-5s';

export interface ResultadoRanking5S {
	ranking: RankingItem[];
	mediaGeral: number;
}

export interface EvolucaoRanking5S {
	setor: SetorRef;
	mes: string;
	pontos: { mes: string; nota: number }[];
}

export async function obterRanking5S(
	params: { mes?: string } = {},
	fetchFn: typeof fetch = fetch
): Promise<ResultadoRanking5S> {
	const qs = montarQuery(params);
	return await apiFetch<ResultadoRanking5S>(`${BASE}${qs}`, { headers: bearer() }, fetchFn);
}

export async function obterEvolucaoRanking5S(
	params: { setor?: string; mes?: string } = {},
	fetchFn: typeof fetch = fetch
): Promise<EvolucaoRanking5S[]> {
	const qs = montarQuery(params);
	return await apiFetch<EvolucaoRanking5S[]>(`${BASE}/evolucao${qs}`, { headers: bearer() }, fetchFn);
}