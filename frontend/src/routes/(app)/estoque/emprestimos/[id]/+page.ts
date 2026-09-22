import { redirect } from '@sveltejs/kit';
import { get } from 'svelte/store';
import type { PageLoad } from './$types';
import type { Emprestimo } from '$lib/types/stock';
import { auth } from '$lib/stores/auth';
import { canSeeLoans } from '$lib/utils/permissions';
import { ApiError } from '$lib/api/client';
import { buscarEmprestimo } from '$lib/api/stock/loans';
import { searchPeople } from '$lib/api/rh';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ params, fetch }) => {
	if (!canSeeLoans(get(auth).user)) {
		throw redirect(303, '/estoque');
	}

	// D-4: o response traz apenas `idPessoa` — nome resolve best-effort via searchPeople.
	let pessoas: Record<string, string> = {};
	try {
		const all = await searchPeople('', fetch);
		for (const p of all) pessoas[String(p.id)] = p.name;
	} catch {
		pessoas = {};
	}

	try {
		const emprestimo = await buscarEmprestimo(params.id, fetch);
		return {
			emprestimo,
			pessoaNome: pessoas[String(emprestimo.idPessoa)] ?? '',
			pessoaFallback: `Pessoa #${emprestimo.idPessoa}`,
			error: null as string | null,
			notFound: false
		};
	} catch (err) {
		const notFound = err instanceof ApiError && err.status === 404;
		return {
			emprestimo: null as Emprestimo | null,
			pessoaNome: '',
			pessoaFallback: '',
			error: err instanceof Error ? err.message : 'Não foi possível carregar o empréstimo',
			notFound
		};
	}
};