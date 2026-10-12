import type { RankingItem, SetorRef } from '$lib/types/producao';

export interface ResultadoRanking5S {
	ranking: RankingItem[];
	mediaGeral: number;
}

export interface EvolucaoRanking5S {
	setor: SetorRef;
	mes: string;
	pontos: { mes: string; nota: number }[];
}

// Sem par no backend (sem controller Java nem rota Laravel): gateadas com mensagem.
export async function obterRanking5S(
	params: { mes?: string } = {},
	fetchFn: typeof fetch = fetch
): Promise<ResultadoRanking5S> {
	throw new Error('Ranking 5S indisponível — sem par no backend (Lote 3).');
}

export async function obterEvolucaoRanking5S(
	params: { setor?: string; mes?: string } = {},
	fetchFn: typeof fetch = fetch
): Promise<EvolucaoRanking5S[]> {
	throw new Error('Evolução do ranking 5S indisponível — sem par no backend (Lote 3).');
}