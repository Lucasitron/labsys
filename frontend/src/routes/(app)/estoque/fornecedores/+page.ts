import type { PageLoad } from './$types';
import type { Fornecedor } from '$lib/types/stock';
import { listarFornecedores } from '$lib/api/stock/suppliers';
import { ApiError } from '$lib/api/client';
import { strParam } from '$lib/utils/stock-url';

export const ssr = false;
export const prerender = false;

function normalize(value: string): string {
	return value.toLowerCase().trim();
}

export const load: PageLoad = async ({ url, fetch }) => {
	const search = strParam(url.searchParams, 'search');

	try {
		const all = await listarFornecedores(fetch);
		const t = normalize(search);
		const fornecedores = t
			? all.filter(
					(f) =>
						normalize(f.nome).includes(t) ||
						normalize(f.cnpj ?? '').includes(t) ||
						normalize(f.contato ?? '').includes(t)
				)
			: all;
		return { search, fornecedores, forbidden: false, error: null as string | null };
	} catch (err) {
		// Est-010: GET /estoque/fornecedores exige perfil ADMIN/BOLSISTA — 403 vira estado amigável.
		if (err instanceof ApiError && err.status === 403) {
			return { search, fornecedores: [] as Fornecedor[], forbidden: true, error: null as string | null };
		}
		return {
			search,
			fornecedores: [] as Fornecedor[],
			forbidden: false,
			error: err instanceof Error ? err.message : 'Não foi possível carregar os fornecedores'
		};
	}
};