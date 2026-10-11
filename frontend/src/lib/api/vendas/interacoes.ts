import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type { Interacao, RegistrarInteracaoPayload } from '$lib/types/vendas';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export interface RawInteracao {
	id: number | string;
	clienteId: number | string;
	tipo: Interacao['tipo'];
	descricao: string;
	dataInteracao: string;
	idUsuario?: number | string | null;
}

export function mapInteracao(raw: RawInteracao): Interacao {
	return {
		id: String(raw.id),
		clienteId: String(raw.clienteId),
		tipo: raw.tipo,
		descricao: raw.descricao,
		dataInteracao: raw.dataInteracao,
		responsavel: ''
	};
}

export async function listInteracoes(
	clienteId: string,
	fetchFn: typeof fetch = fetch
): Promise<Interacao[]> {
	const raw = await apiFetch<RawInteracao[]>(
		`/vendas/interacoes/${clienteId}`,
		{ headers: bearer() },
		fetchFn
	);
	return raw.map(mapInteracao);
}

export async function registrarInteracao(
	payload: RegistrarInteracaoPayload,
	fetchFn: typeof fetch = fetch
): Promise<Interacao> {
	const raw = await apiFetch<RawInteracao>(
		'/vendas/interacoes',
		{
			method: 'POST',
			headers: bearer(),
			body: JSON.stringify({
				clienteId: Number(payload.clienteId),
				tipo: payload.tipo,
				descricao: payload.descricao,
				...(payload.dataInteracao ? { dataInteracao: payload.dataInteracao } : {})
			})
		},
		fetchFn
	);
	return mapInteracao(raw);
}
