import type { PageLoad } from './$types';
import { getPerfil } from '$lib/api/configuracoes/perfil';
import type { Perfil } from '$lib/types/configuracoes';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ fetch }) => {
	try {
		const perfil: Perfil = await getPerfil(fetch);
		return { perfil, error: null as string | null };
	} catch (err) {
		return {
			perfil: null,
			error: err instanceof Error ? err.message : 'Não foi possível carregar o perfil'
		};
	}
};