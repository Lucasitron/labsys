import type { PageLoad } from './$types';
import {
	obterProjetos,
	obterTarefas,
	PADRAO_TAMANHO_PAGINA,
	paginaDaUrl,
	tamanhoDaPaginaDaUrl
} from '$lib/api/producao/client';
import type { NomeFunc, Paginado, Prioridade, Projeto, Tarefa, TarefaStatus } from '$lib/types/producao';

export const ssr = false;
export const prerender = false;

export type VisaoTarefas = 'kanban' | 'lista';

export interface TarefasParams {
	search: string;
	status: TarefaStatus[];
	prioridade: Prioridade[];
	responsavel: string[];
	view: VisaoTarefas;
	page: number;
	pageSize: number;
}

export const STATUS_TAREFA = ['Pendente', 'Em andamento', 'Concluída', 'Atrasada', 'Bloqueada'] as const;
export const PRIORIDADES = ['Baixa', 'Média', 'Moderada', 'Alta'] as const;

function lerParametros(url: URL): TarefasParams {
	return {
		search: url.searchParams.get('search') ?? '',
		status: url.searchParams.getAll('status').filter((v): v is TarefaStatus => {
			return (STATUS_TAREFA as readonly string[]).includes(v);
		}),
		prioridade: url.searchParams.getAll('prioridade').filter((v): v is Prioridade => {
			return (PRIORIDADES as readonly string[]).includes(v);
		}),
		responsavel: url.searchParams.getAll('responsavel'),
		view: url.searchParams.get('view') === 'lista' ? 'lista' : 'kanban',
		page: paginaDaUrl(url, 1),
		pageSize: tamanhoDaPaginaDaUrl(url, PADRAO_TAMANHO_PAGINA)
	};
}

function pessoasDe(tarefas: Tarefa[], projetos: Projeto[]): NomeFunc[] {
	const mapa = new Map<string, NomeFunc>();
	for (const tarefa of tarefas) mapa.set(tarefa.responsavel.id, tarefa.responsavel);
	for (const projeto of projetos) mapa.set(projeto.responsavel.id, projeto.responsavel);
	return [...mapa.values()];
}

export const load: PageLoad = async ({ url, fetch }) => {
	const params = lerParametros(url);
	let tarefas: Paginado<Tarefa> | null = null;
	let projetos: Projeto[] = [];
	let error: string | null = null;

	try {
		tarefas = await obterTarefas(
			{ page: params.page, pageSize: params.pageSize },
			fetch
		);
	} catch (err) {
		error = err instanceof Error ? err.message : 'Não foi possível carregar as tarefas';
	}

	try {
		const resultado = await obterProjetos({ page: 1, pageSize: 100 }, fetch);
		projetos = resultado.dados;
	} catch {
		projetos = [];
	}

	return { params, tarefas, projetos, responsaveis: pessoasDe(tarefas?.dados ?? [], projetos), error };
};