import { bearer } from '$lib/api/bearer';
import { apiFetch } from '$lib/api/client';
import {
	CATEGORIA_TIPO_PARA_UPPER,
	UPPER_PARA_CATEGORIA_TIPO
} from '$lib/utils/financeiro-map';
import type { CategoriaFinanceira, CategoriaTipo, CreateCategoriaPayload } from '$lib/types/financeiro';

const BASE = '/financeiro/categorias';


export interface RawCategoria {
	id: number | string;
	nome: string;
	tipo: string;
	descricao?: string | null;
}

export function mapCategoria(raw: RawCategoria): CategoriaFinanceira {
	return {
		id: String(raw.id),
		nome: raw.nome,
		tipo: (UPPER_PARA_CATEGORIA_TIPO[raw.tipo] ?? 'Despesa') as CategoriaTipo,
		...(raw.descricao ? { descricao: raw.descricao } : {})
	};
}

export async function listCategorias(fetchFn: typeof fetch = fetch): Promise<CategoriaFinanceira[]> {
	const raw = await apiFetch<RawCategoria[] | { data: RawCategoria[] }>(
		BASE,
		{ headers: bearer() },
		fetchFn
	);
	const lista = Array.isArray(raw) ? raw : (raw.data ?? []);
	return lista.map(mapCategoria);
}

export async function createCategoria(
	payload: CreateCategoriaPayload,
	fetchFn: typeof fetch = fetch
): Promise<CategoriaFinanceira> {
	const raw = await apiFetch<RawCategoria>(
		BASE,
		{
			method: 'POST',
			headers: bearer(),
			body: JSON.stringify({
				nome: payload.nome,
				tipo: CATEGORIA_TIPO_PARA_UPPER[payload.tipo],
				...(payload.descricao ? { descricao: payload.descricao } : {})
			})
		},
		fetchFn
	);
	return mapCategoria(raw);
}
