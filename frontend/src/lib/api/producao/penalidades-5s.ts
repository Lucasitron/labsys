import type { Paginado, Penalidade5S, PenalidadeStatus, PenalidadeTipo } from '$lib/types/producao';

export type PenalidadeFiltros = {
	tipo?: PenalidadeTipo[];
	situacao?: PenalidadeStatus[];
	page?: number;
	pageSize?: number;
};

export interface CriarPenalidadePayload {
	membroId: string;
	tipo: PenalidadeTipo;
	motivo: string;
}

// Sem par no backend (sem controller Java nem rota Laravel): gateadas com mensagem.
export async function obterPenalidades5S(
	params: PenalidadeFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Penalidade5S>> {
	throw new Error('Penalidades 5S indisponíveis — sem par no backend (Lote 3).');
}

export function criarPenalidade5S(payload: CriarPenalidadePayload): Promise<Penalidade5S> {
	return Promise.reject(
		new Error('Criar penalidade 5S indisponível — sem par no backend (Lote 3).')
	);
}

export function marcarPenalidade5SCumprida(id: string): Promise<Penalidade5S> {
	return Promise.reject(
		new Error('Baixar penalidade 5S indisponível — sem par no backend (Lote 3).')
	);
}