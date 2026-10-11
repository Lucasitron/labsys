import type { BomResponse, CreateBomPayload, RegistrarConsumoPayload } from '$lib/types/stock';
import { stockFetch } from './request';

function mapBom(raw: BomResponse): BomResponse {
	return {
		...raw,
		itens: raw.itens.map((i) => ({
			...i,
			quantidadePrevista: Number(i.quantidadePrevista),
			quantidadeReal: Number(i.quantidadeReal)
		}))
	};
}

export async function buscarBom(id: string, fetchFn: typeof fetch = fetch): Promise<BomResponse> {
	return mapBom(await stockFetch<BomResponse>(`/estoque/boms/${id}`, {}, fetchFn));
}

export async function criarBom(payload: CreateBomPayload): Promise<BomResponse> {
	return mapBom(await stockFetch<BomResponse>('/estoque/boms', {
		method: 'POST',
		body: JSON.stringify(payload)
	}));
}

export async function atualizarBom(
	id: string,
	payload: CreateBomPayload
): Promise<BomResponse> {
	return mapBom(await stockFetch<BomResponse>(`/estoque/boms/${id}`, {
		method: 'PUT',
		body: JSON.stringify(payload)
	}));
}

export async function registrarConsumo(
	id: string,
	payload: RegistrarConsumoPayload
): Promise<void> {
	await stockFetch<unknown>(`/estoque/boms/${id}/consumo`, {
		method: 'POST',
		body: JSON.stringify({
			itens: payload.itens.map((i) => ({
				idItem: i.idItem,
				quantidadeConsumida: i.quantidade
			}))
		})
	});
}
