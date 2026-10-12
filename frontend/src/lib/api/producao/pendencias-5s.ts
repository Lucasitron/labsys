import type { GravidadePendencia, Paginado, Pendencia5S, PenalidadeTipo, SituacaoPendencia } from '$lib/types/producao';

export type PendenciaFiltros = {
	gravidade?: GravidadePendencia[];
	setor?: string[];
	situacao?: SituacaoPendencia[];
	page?: number;
	pageSize?: number;
};

export interface ResolverPendenciaPayload {
	observacao?: string;
}

export interface RegistrarPenalidadePayload {
	tipo: PenalidadeTipo;
	motivo: string;
}

// Sem par no backend (sem controller Java nem rota Laravel): gateadas com mensagem.
export async function obterPendencias5S(
	params: PendenciaFiltros = {},
	fetchFn: typeof fetch = fetch
): Promise<Paginado<Pendencia5S>> {
	throw new Error('Pendências 5S indisponíveis — sem par no backend (Lote 3).');
}

export function resolverPendencia5S(id: string, payload: ResolverPendenciaPayload): Promise<Pendencia5S> {
	return Promise.reject(
		new Error('Resolver pendência 5S indisponível — sem par no backend (Lote 3).')
	);
}

export function registrarPenalidadeParaPendencia5S(
	id: string,
	payload: RegistrarPenalidadePayload
): Promise<Pendencia5S> {
	return Promise.reject(
		new Error('Penalidade de pendência 5S indisponível — sem par no backend (Lote 3).')
	);
}