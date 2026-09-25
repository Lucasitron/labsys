import type { PageLoad } from './$types';
import { obterResumo } from '$lib/api/producao/client';
import type { ResumoProducao } from '$lib/types/producao';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ fetch }) => {
	try {
		const resumo: ResumoProducao = await obterResumo(fetch);
		return { resumo, error: null as string | null };
	} catch (err) {
		return {
			resumo: null,
			error: err instanceof Error ? err.message : 'Não foi possível carregar o resumo'
		};
	}
};