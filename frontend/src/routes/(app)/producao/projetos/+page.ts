import type { PageLoad } from './$types';
import {
	obterProjetos,
	PADRAO_TAMANHO_PAGINA,
	paginaDaUrl,
	tamanhoDaPaginaDaUrl
} from '$lib/api/producao/client';
import type { NomeFunc, Paginado, Projeto, ProjetoStatus } from '$lib/types/producao';

export const ssr = false;
export const prerender = false;

export interface ProjetosParams {
	search: string;
	status: ProjetoStatus[];
	page: number;
	pageSize: number;
}

function lerParametros(url: URL): ProjetosParams {
	return {
		search: url.searchParams.get('search') ?? '',
		status: url.searchParams.getAll('status').filter((v): v is ProjetoStatus => {
			return v === 'Planejado' || v === 'Em andamento' || v === 'Concluído' || v === 'Cancelado';
		}),
		page: paginaDaUrl(url, 1),
		pageSize: tamanhoDaPaginaDaUrl(url, PADRAO_TAMANHO_PAGINA)
	};
}

function pessoasDe(projetos: Projeto[]): NomeFunc[] {
	const mapa = new Map<string, NomeFunc>();
	for (const projeto of projetos) mapa.set(projeto.responsavel.id, projeto.responsavel);
	return [...mapa.values()];
}

export const load: PageLoad = async ({ url, fetch }) => {
	const params = lerParametros(url);
	let projetos: Paginado<Projeto> | null = null;
	let error: string | null = null;

	try {
		projetos = await obterProjetos(
			{ page: params.page, pageSize: params.pageSize },
			fetch
		);
	} catch (err) {
		error = err instanceof Error ? err.message : 'Não foi possível carregar os projetos';
	}

	return { params, projetos, responsaveis: pessoasDe(projetos?.dados ?? []), error };
};