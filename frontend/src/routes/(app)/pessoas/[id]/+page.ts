import type { PageLoad } from './$types';
import { ApiError } from '$lib/api/client';
import { getPessoa, nivelDoServidor } from '$lib/api/rh/pessoas';
import { nivelPorPessoa } from '$lib/api/rh/funcionarios';
import { strParam } from '$lib/utils/stock-url';
import type { PersonDetail } from '$lib/types/rh';

export const ssr = false;
export const prerender = false;

const TABS = ['visao-geral', 'horas', 'treinamentos', 'historico'];

export const load: PageLoad = async ({ params, url, fetch }) => {
	const tabRaw = strParam(url.searchParams, 'tab');
	const tab = TABS.includes(tabRaw) ? tabRaw : 'visao-geral';

	try {
		const [pessoa, niveis] = await Promise.all([
			getPessoa(params.id, fetch),
			nivelPorPessoa(fetch)
		]);
		const nivel = nivelDoServidor(niveis.get(params.id));
		const detalhe: PersonDetail = nivel ? { ...pessoa, nivel } : pessoa;
		return { id: params.id, tab, pessoa: detalhe, notFound: false, error: null as string | null };
	} catch (err) {
		if (err instanceof ApiError && err.status === 404) {
			return { id: params.id, tab, pessoa: null, notFound: true, error: null as string | null };
		}
		return {
			id: params.id,
			tab,
			pessoa: null,
			notFound: false,
			error: 'Não foi possível carregar a pessoa'
		};
	}
};
