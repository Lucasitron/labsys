import type { PageLoad } from './$types';
import { listLancamentos } from '$lib/api/financeiro/lancamentos';
import { listCategorias } from '$lib/api/financeiro/categorias';
import type { CategoriaFinanceira, LancamentosResult } from '$lib/types/financeiro';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ fetch }) => {
	const [principal, categorias] = await Promise.allSettled([
		listLancamentos({ page: 1, pageSize: 10 }, fetch),
		listCategorias(fetch)
	]);

	return {
		resultado: principal.status === 'fulfilled' ? (principal.value as LancamentosResult) : null,
		error:
			principal.status === 'rejected' ? 'Não foi possível carregar o resumo financeiro' : (null as string | null),
		categorias: categorias.status === 'fulfilled' ? (categorias.value as CategoriaFinanceira[]) : [],
		categoriasError:
			categorias.status === 'rejected'
				? 'Não foi possível carregar as categorias'
				: (null as string | null)
	};
};
