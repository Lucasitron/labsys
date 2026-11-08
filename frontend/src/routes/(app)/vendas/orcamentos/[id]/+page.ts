import type { PageLoad } from './$types';
import { ApiError } from '$lib/api/client';
import { getOrcamento } from '$lib/api/vendas/orcamentos';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ params, fetch }) => {
	try {
		const orcamento = await getOrcamento(params.id, fetch);
		return { id: params.id, orcamento, notFound: false, error: null as string | null };
	} catch (err) {
		if (err instanceof ApiError && err.status === 404) {
			return { id: params.id, orcamento: null, notFound: true, error: null as string | null };
		}
		return {
			id: params.id,
			orcamento: null,
			notFound: false,
			error: 'Não foi possível carregar o orçamento'
		};
	}
};
