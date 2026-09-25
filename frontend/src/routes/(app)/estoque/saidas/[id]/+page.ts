import type { PageLoad } from './$types';
import type { SaidaResponse } from '$lib/types/stock';
import { ApiError } from '$lib/api/client';
import { buscarSaida } from '$lib/api/stock/movements';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ params, fetch }) => {
	try {
		const saida = await buscarSaida(params.id, fetch);
		return { saida, error: null as string | null, notFound: false };
	} catch (err) {
		const notFound = err instanceof ApiError && err.status === 404;
		return {
			saida: null as SaidaResponse | null,
			error: err instanceof Error ? err.message : 'Não foi possível carregar a saída',
			notFound
		};
	}
};