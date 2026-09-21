import type { PageLoad } from './$types';
import type { Emprestimo, LoanTab } from '$lib/types/stock';
import { listarAtrasados } from '$lib/api/stock/loans';
import { searchPeople } from '$lib/api/rh';
import { strParam } from '$lib/utils/stock-url';
import { LOAN_TABS } from '$lib/utils/stock-status';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ url, fetch }) => {
	const raw = strParam(url.searchParams, 'tab');
	const tab: LoanTab =
		raw === 'ativos' || raw === 'atrasados' || raw === 'historico' ? raw : 'atrasados';

	// D-4: o response de empréstimo traz apenas `idPessoa` — nome resolve best-effort via searchPeople.
	let pessoas: Record<string, string> = {};
	try {
		const all = await searchPeople('', fetch);
		for (const p of all) pessoas[String(p.id)] = p.name;
	} catch {
		pessoas = {};
	}

	try {
		const atrasados = await listarAtrasados(fetch);
		return { tab, atrasados, pessoas, error: null as string | null };
	} catch (err) {
		return {
			tab,
			atrasados: [],
			pessoas,
			error: err instanceof Error ? err.message : 'Não foi possível carregar os empréstimos'
		};
	}
};