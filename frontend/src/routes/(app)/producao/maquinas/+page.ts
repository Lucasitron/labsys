import type { PageLoad } from './$types';
import {
	obterMaquinas,
	PADRAO_TAMANHO_PAGINA,
	paginaDaUrl,
	tamanhoDaPaginaDaUrl
} from '$lib/api/producao/client';
import type { Cartao5S, Maquina, NomeFunc, Paginado } from '$lib/types/producao';

export const ssr = false;
export const prerender = false;

export interface MaquinasParams {
	search: string;
	page: number;
	pageSize: number;
	categoria: string[];
	cartao: Cartao5S[];
}

const CARTOES: Cartao5S[] = ['Verde', 'Amarelo', 'Vermelho'];

function lerParametros(url: URL): MaquinasParams {
	return {
		search: url.searchParams.get('search') ?? '',
		page: paginaDaUrl(url, 1),
		pageSize: tamanhoDaPaginaDaUrl(url, PADRAO_TAMANHO_PAGINA),
		categoria: url.searchParams.getAll('categoria').filter((v) => v !== ''),
		cartao: url.searchParams.getAll('cartao').filter((v): v is Cartao5S => {
			return v === 'Verde' || v === 'Amarelo' || v === 'Vermelho';
		})
	};
}

export const load: PageLoad = async ({ url, fetch }) => {
	const params = lerParametros(url);
	let maquinas: Paginado<Maquina> | null = null;
	let error: string | null = null;

	try {
		maquinas = await obterMaquinas(
			{ page: params.page, pageSize: params.pageSize },
			fetch
		);
	} catch (err) {
		error = err instanceof Error ? err.message : 'Não foi possível carregar as máquinas';
	}

	const dados = maquinas?.dados ?? [];
	const responsaveis = new Map<string, NomeFunc>();
	for (const maquina of dados) responsaveis.set(maquina.responsavelManutencao.id, maquina.responsavelManutencao);

	return {
		params,
		maquinas,
		responsaveisManutencao: [...responsaveis.values()],
		error
	};
};

export { CARTOES };