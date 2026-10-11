import type { ChaveParametro5S, Parametro5S } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer, unwrapLista } from './client';

const BASE = '/producao/parametros-5s';

export interface RawParametro5S {
	id: number | string;
	chave: string;
	valor: string;
	descricao?: string | null;
}

export function mapParametro(raw: RawParametro5S): Parametro5S {
	return {
		id: String(raw.id),
		chave: raw.chave as ChaveParametro5S,
		valor: raw.valor,
		...(raw.descricao ? { descricao: raw.descricao } : {})
	};
}

export async function obterParametros5S(fetchFn: typeof fetch = fetch): Promise<Parametro5S[]> {
	const res = await apiFetch<RawParametro5S[] | { data: RawParametro5S[] }>(
		BASE,
		{ headers: bearer() },
		fetchFn
	);
	return unwrapLista(res).map(mapParametro);
}

export function atualizarParametro5S(
	id: string,
	payload: { valor: string; descricao?: string }
): Promise<Parametro5S> {
	return apiFetch<Parametro5S>(`${BASE}/${encodeURIComponent(id)}`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(payload)
	});
}

// Legado sem par (PUT em coleção inexistente no back): gateado com mensagem.
export function atualizarParametros5S(): Promise<never> {
	return Promise.reject(
		new Error('Use a atualização por parâmetro (PUT /producao/parametros-5s/{id}).')
	);
}
