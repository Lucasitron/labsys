import type { PageLoad } from './$types';
import {
	obterMesas,
	obterProjetos,
	PADRAO_TAMANHO_PAGINA,
	paginaDaUrl,
	tamanhoDaPaginaDaUrl
} from '$lib/api/producao/client';
import type { Mesa5S, NomeFunc, Paginado, Projeto, SituacaoMesas } from '$lib/types/producao';

export const ssr = false;
export const prerender = false;

export interface MesasParams {
	search: string;
	mes: string;
	situacao: SituacaoMesas[];
	page: number;
	pageSize: number;
}

const SITUACOES: SituacaoMesas[] = ['Aprovada', 'Pendente'];

function lerParametros(url: URL): MesasParams {
	return {
		search: url.searchParams.get('search') ?? '',
		mes: url.searchParams.get('mes') ?? '',
		situacao: url.searchParams
			.getAll('situacao')
			.filter((v): v is SituacaoMesas => v === 'Aprovada' || v === 'Pendente'),
		page: paginaDaUrl(url, 1),
		pageSize: tamanhoDaPaginaDaUrl(url, PADRAO_TAMANHO_PAGINA)
	};
}

function pessoasDe(mesas: Mesa5S[], projetos: Projeto[]): NomeFunc[] {
	const mapa = new Map<string, NomeFunc>();
	for (const mesa of mesas) mapa.set(mesa.membro.id, mesa.membro);
	for (const projeto of projetos) mapa.set(projeto.responsavel.id, projeto.responsavel);
	return [...mapa.values()];
}

export const load: PageLoad = async ({ url, fetch }) => {
	const params = lerParametros(url);
	let mesas: Paginado<Mesa5S> | null = null;
	let projetos: Projeto[] = [];
	let error: string | null = null;

	try {
		mesas = await obterMesas(
			{ page: params.page, pageSize: params.pageSize, mes: params.mes || undefined },
			fetch
		);
	} catch (err) {
		error = err instanceof Error ? err.message : 'Não foi possível carregar as mesas';
	}

	try {
		const resultado = await obterProjetos({ page: 1, pageSize: 100 }, fetch);
		projetos = resultado.dados;
	} catch {
		projetos = [];
	}

	return { params, mesas, projetos, membros: pessoasDe(mesas?.dados ?? [], projetos), error };
};

export { SITUACOES };