import { error } from '@sveltejs/kit';
import type { PageLoad } from './$types';
import {
	obterMaquinaPorId,
	obterPreventivasDaMaquina,
	obterChamadosDaMaquina
} from '$lib/api/producao/client';
import type { ChamadoMaquina, Maquina, Preventiva } from '$lib/types/producao';

export const ssr = false;
export const prerender = false;

export type AbaMaquina =
	| 'geral'
	| 'documentacao'
	| 'receitas'
	| 'preflight'
	| 'manutencao'
	| 'chamados';

export const ABAS_MAQUINA: { id: AbaMaquina; label: string }[] = [
	{ id: 'geral', label: 'Geral' },
	{ id: 'documentacao', label: 'Documentação' },
	{ id: 'receitas', label: 'Receitas' },
	{ id: 'preflight', label: 'Pré-flight' },
	{ id: 'manutencao', label: 'Manutenção' },
	{ id: 'chamados', label: 'Chamados' }
];

function abaDaUrl(url: URL): AbaMaquina {
	const aba = url.searchParams.get('tab');
	if (
		aba === 'geral' ||
		aba === 'documentacao' ||
		aba === 'receitas' ||
		aba === 'preflight' ||
		aba === 'manutencao' ||
		aba === 'chamados'
	) {
		return aba;
	}
	return 'geral';
}

export const load: PageLoad = async ({ params, url, fetch }) => {
	const id = params.id;

	let maquina: Maquina | null = null;
	let preventivas: Preventiva[] = [];
	let chamados: ChamadoMaquina[] = [];
	let erroPagina: string | null = null;
	let erroPreventivas: string | null = null;
	let erroChamados: string | null = null;

	try {
		maquina = await obterMaquinaPorId(id, fetch);
	} catch (err) {
		erroPagina = err instanceof Error ? err.message : 'Não foi possível carregar a máquina';
	}

	try {
		preventivas = await obterPreventivasDaMaquina(id, fetch);
	} catch (err) {
		erroPreventivas = err instanceof Error ? err.message : 'Não foi possível carregar as preventivas';
	}

	try {
		chamados = await obterChamadosDaMaquina(id, fetch);
	} catch (err) {
		erroChamados = err instanceof Error ? err.message : 'Não foi possível carregar os chamados';
	}

	if (!maquina && erroPagina) {
		error(404, { message: 'Máquina não encontrada' });
	}

	return {
		maquina,
		preventivas,
		chamados,
		aba: abaDaUrl(url),
		erroPagina,
		erroPreventivas,
		erroChamados
	};
};