import type { PageLoad } from './$types';
import { listarUsuarios } from '$lib/api/configuracoes/usuarios';
import { intParam, strParam } from '$lib/utils/stock-url';
import type { Nivel, Paginado, SituacaoUsuario, Usuario } from '$lib/types/configuracoes';

export interface UsuariosFilterState {
	page: number;
	search: string;
	nivel: string;
	situacao: string;
}

const NIVEIS: Nivel[] = [0, 1, 2, 3, 4];
const SITUACOES: SituacaoUsuario[] = ['Ativo', 'Pendente', 'Desativado'];

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ url, fetch }) => {
	const searchParams = url.searchParams;

	const params: UsuariosFilterState = {
		page: intParam(searchParams, 'page', 1),
		search: strParam(searchParams, 'search'),
		nivel: strParam(searchParams, 'nivel'),
		situacao: strParam(searchParams, 'situacao')
	};

	const nivelNumber = Number(params.nivel);
	const nivel = NIVEIS.includes(nivelNumber as Nivel) ? (nivelNumber as Nivel) : undefined;
	const situacao = SITUACOES.includes(params.situacao as SituacaoUsuario)
		? (params.situacao as SituacaoUsuario)
		: undefined;

	try {
		const resultado: Paginado<Usuario> = await listarUsuarios(
			{
				page: params.page,
				search: params.search || undefined,
				nivel,
				situacao
			},
			fetch
		);
		return { params, resultado, error: null as string | null };
	} catch (err) {
		return {
			params,
			resultado: null,
			error: err instanceof Error ? err.message : 'Não foi possível carregar os usuários'
		};
	}
};