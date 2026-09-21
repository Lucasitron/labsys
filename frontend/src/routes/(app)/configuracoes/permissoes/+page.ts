import type { PageLoad } from './$types';
import { obterPermissoes } from '$lib/api/configuracoes/permissoes';
import type { PermissaoMatriz } from '$lib/types/configuracoes';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ fetch }) => {
	try {
		const matriz: PermissaoMatriz = await obterPermissoes(fetch);
		return { matriz, error: null as string | null };
	} catch (err) {
		return {
			matriz: null,
			error: err instanceof Error ? err.message : 'Não foi possível carregar as permissões'
		};
	}
};