import type { PageLoad } from './$types';
import { listPessoas, nivelDoServidor } from '$lib/api/rh/pessoas';
import { nivelPorPessoa } from '$lib/api/rh/funcionarios';
import { intParam, strParam } from '$lib/utils/stock-url';
import type { PessoasResult } from '$lib/types/rh';

export interface PessoasFilterState {
	search: string;
	setor: string;
	nivel: string;
	status: string;
	page: number;
	pageSize: number;
}

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ url, fetch }) => {
	const searchParams = url.searchParams;

	const params: PessoasFilterState = {
		search: strParam(searchParams, 'search'),
		setor: strParam(searchParams, 'setor'),
		nivel: strParam(searchParams, 'nivel'),
		status: strParam(searchParams, 'status'),
		page: intParam(searchParams, 'page', 1),
		pageSize: intParam(searchParams, 'pageSize', 10, 10, 100)
	};

	try {
		const [resultado, niveis] = await Promise.all([
			listPessoas(
				{
					search: params.search || undefined,
					setor: params.setor || undefined,
					nivel: params.nivel || undefined,
					status: params.status || undefined,
					page: params.page,
					pageSize: params.pageSize
				},
				fetch
			),
			nivelPorPessoa(fetch)
		]);
		const people = resultado.people.map((pessoa) => {
			const nivel = nivelDoServidor(niveis.get(pessoa.id));
			return nivel ? { ...pessoa, nivel } : pessoa;
		});
		return { params, resultado: { ...resultado, people }, error: null as string | null };
	} catch {
		return {
			params,
			resultado: null,
			error: 'Não foi possível carregar as pessoas'
		};
	}
};
