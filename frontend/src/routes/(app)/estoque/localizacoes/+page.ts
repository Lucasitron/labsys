import type { PageLoad } from './$types';
import type { LocalizacaoResp } from '$lib/types/stock';
import { listarLocalizacoes } from '$lib/api/stock/locations';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ fetch }) => {
	try {
		const localizacoes = await listarLocalizacoes(fetch);
		return { localizacoes, error: null as string | null };
	} catch (err) {
		return {
			localizacoes: [] as LocalizacaoResp[],
			error: err instanceof Error ? err.message : 'Não foi possível carregar as localizações'
		};
	}
};