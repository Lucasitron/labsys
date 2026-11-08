import type { PageLoad } from './$types';
import type { EntradaResponse } from '$lib/types/stock';
import { ApiError } from '$lib/api/client';
import { buscarEntrada } from '$lib/api/stock/movements';

export const ssr = false;
export const prerender = false;

// O backend (EntradaResponse do estoque-service) envia `nomeFornecedor` e `valorTotal`
// além do shape tipado no frontend. Exposto localmente na rota (sem alterar types/API) — dívida D-2/D-3.
export interface EntradaDetalhe extends EntradaResponse {
	nomeFornecedor?: string | null;
	valorTotal?: number | null;
}

export const load: PageLoad = async ({ params, fetch }) => {
	try {
		const raw = await buscarEntrada(params.id, fetch);
		const entrada = raw as unknown as EntradaDetalhe;
		return { entrada, error: null as string | null, notFound: false };
	} catch (err) {
		const notFound = err instanceof ApiError && err.status === 404;
		return {
			entrada: null as EntradaDetalhe | null,
			error: err instanceof Error ? err.message : 'Não foi possível carregar a entrada',
			notFound
		};
	}
};