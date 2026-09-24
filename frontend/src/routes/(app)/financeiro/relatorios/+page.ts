import type { PageLoad } from './$types';
import {
	getCustoMaquina,
	getDoacoesDespesas,
	getDre,
	getFluxoCaixa,
	getInadimplencia,
	getLucratividade
} from '$lib/api/financeiro/relatorios';
import type {
	CustoMaquina,
	DoacoesDespesas,
	Dre,
	FluxoCaixa,
	Inadimplencia,
	Lucratividade,
	RelatorioId
} from '$lib/types/financeiro';

export const ssr = false;
export const prerender = false;

const RELATORIOS_VALIDOS: RelatorioId[] = [
	'fluxo',
	'dre',
	'lucratividade',
	'inadimplencia',
	'doacoes',
	'custo-maquina'
];

export type RelatorioDados =
	| FluxoCaixa
	| Dre
	| Lucratividade
	| Inadimplencia
	| DoacoesDespesas
	| CustoMaquina;

export const PERIODO_PADRAO = '2026-09';

export const load: PageLoad = async ({ url, fetch }) => {
	const bruto = (url.searchParams.get('relatorio') ?? '').trim();
	const relatorio: RelatorioId = (RELATORIOS_VALIDOS as string[]).includes(bruto)
		? (bruto as RelatorioId)
		: 'fluxo';
	const periodo = (url.searchParams.get('periodo') ?? '').trim() || PERIODO_PADRAO;

	let dados: RelatorioDados | null = null;
	let error: string | null = null;
	try {
		switch (relatorio) {
			case 'fluxo':
				dados = await getFluxoCaixa({ periodo }, fetch);
				break;
			case 'dre':
				dados = await getDre({ periodo }, fetch);
				break;
			case 'lucratividade':
				dados = await getLucratividade({ periodo }, fetch);
				break;
			case 'inadimplencia':
				dados = await getInadimplencia({ periodo }, fetch);
				break;
			case 'doacoes':
				dados = await getDoacoesDespesas({ periodo }, fetch);
				break;
			case 'custo-maquina':
				dados = await getCustoMaquina({ periodo }, fetch);
				break;
		}
	} catch {
		error = 'Não foi possível carregar o relatório';
	}

	return { relatorio, periodo, dados, error };
};
