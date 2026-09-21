import type { PageLoad } from './$types';
import { listarTokens, obterSistema } from '$lib/api/configuracoes/sistema';
import type { ParametroSistema, TokenIntegracao } from '$lib/types/configuracoes';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ fetch }) => {
	try {
		const [sistema, tokens] = await Promise.all([obterSistema(fetch), listarTokens(fetch)]);
		return { sistema, tokens, error: null as string | null };
	} catch (err) {
		return {
			sistema: null,
			tokens: null,
			error:
				err instanceof Error ? err.message : 'Não foi possível carregar os dados do sistema'
		};
	}
};