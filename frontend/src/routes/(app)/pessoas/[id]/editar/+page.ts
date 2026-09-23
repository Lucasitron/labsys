import type { PageLoad } from './$types';
import { ApiError } from '$lib/api/client';
import { getPessoa } from '$lib/api/rh/pessoas';
import type { PersonDetail } from '$lib/types/rh';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ params, fetch }) => {
	try {
		const pessoa: PersonDetail = await getPessoa(params.id, fetch);
		return { id: params.id, pessoa, notFound: false, error: null as string | null };
	} catch (err) {
		if (err instanceof ApiError && err.status === 404) {
			return { id: params.id, pessoa: null, notFound: true, error: null as string | null };
		}
		return {
			id: params.id,
			pessoa: null,
			notFound: false,
			error: err instanceof Error ? err.message : 'Não foi possível carregar a pessoa'
		};
	}
};
