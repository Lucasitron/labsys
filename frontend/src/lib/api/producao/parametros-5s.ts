import type { ChaveParametro5S, Parametro5S } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer } from './client';

const BASE = '/producao/parametros-5s';

export interface Parametro5SAtualizacao {
	chave: ChaveParametro5S;
	valor: number | string | boolean;
}

export async function obterParametros5S(fetchFn: typeof fetch = fetch): Promise<Parametro5S[]> {
	return await apiFetch<Parametro5S[]>(BASE, { headers: bearer() }, fetchFn);
}

export function atualizarParametros5S(parametros: Parametro5SAtualizacao[]): Promise<Parametro5S[]> {
	return apiFetch<Parametro5S[]>(BASE, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(parametros)
	});
}