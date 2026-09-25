import { error } from '@sveltejs/kit';
import type { PageLoad } from './$types';
import { obterProjetoPorId, obterMateriaisDoProjeto, obterTarefas } from '$lib/api/producao/client';
import type { MaterialProjeto, Paginado, Projeto, Tarefa } from '$lib/types/producao';

export const ssr = false;
export const prerender = false;

export type AbaProjeto = 'geral' | 'documentacao' | 'materiais' | 'tarefas';

export const ABAS_PROJETO: { id: AbaProjeto; label: string }[] = [
	{ id: 'geral', label: 'Geral' },
	{ id: 'documentacao', label: 'Documentação' },
	{ id: 'materiais', label: 'Materiais' },
	{ id: 'tarefas', label: 'Tarefas' }
];

function abaDaUrl(url: URL): AbaProjeto {
	const aba = url.searchParams.get('tab');
	if (aba === 'geral' || aba === 'documentacao' || aba === 'materiais' || aba === 'tarefas') return aba;
	return 'geral';
}

export const load: PageLoad = async ({ params, url, fetch }) => {
	const id = params.id;

	let projeto: Projeto | null = null;
	let materiais: MaterialProjeto[] = [];
	let tarefas: Paginado<Tarefa> | null = null;
	let erroPagina: string | null = null;
	let erroMateriais: string | null = null;
	let erroTarefas: string | null = null;

	try {
		projeto = await obterProjetoPorId(id, fetch);
	} catch (err) {
		erroPagina = err instanceof Error ? err.message : 'Não foi possível carregar o projeto';
	}

	try {
		materiais = await obterMateriaisDoProjeto(id, fetch);
	} catch (err) {
		erroMateriais = err instanceof Error ? err.message : 'Não foi possível carregar os materiais';
	}

	try {
		tarefas = await obterTarefas({ projetoId: id, page: 1, pageSize: 100 }, fetch);
	} catch (err) {
		erroTarefas = err instanceof Error ? err.message : 'Não foi possível carregar as tarefas';
	}

	if (!projeto && erroPagina) {
		error(404, { message: 'Projeto não encontrado' });
	}

	return {
		projeto,
		materiais,
		tarefas: tarefas?.dados ?? [],
		aba: abaDaUrl(url),
		erroPagina,
		erroMateriais,
		erroTarefas
	};
};