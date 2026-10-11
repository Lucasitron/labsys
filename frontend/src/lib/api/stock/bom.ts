import type { BomResponse, CreateBomPayload, RegistrarConsumoPayload } from '$lib/types/stock';
import { stockFetch } from './request';

function mapBom(raw: BomResponse): BomResponse {
	return {
		...raw,
		versao: Number(raw.versao),
		itens: raw.itens.map((i) => ({
			...i,
			quantidadePrevista: Number(i.quantidadePrevista),
			quantidadeReal: Number(i.quantidadeReal)
		}))
	};
}

function paraPayloadBom(payload: CreateBomPayload): Record<string, unknown> {
	return {
		...payload,
		idProdutoServico: Number(payload.idProdutoServico),
		...(payload.versao !== undefined ? { versao: Number(payload.versao) } : {}),
		itens: payload.itens.map((i) => ({
			idItem: Number(i.idItem),
			quantidadePrevista: Number(i.quantidadePrevista)
		}))
	};
}

export async function buscarBom(id: string, fetchFn: typeof fetch = fetch): Promise<BomResponse> {
	return mapBom(await stockFetch<BomResponse>(`/estoque/boms/${id}`, {}, fetchFn));
}

export async function criarBom(payload: CreateBomPayload): Promise<BomResponse> {
	return mapBom(await stockFetch<BomResponse>('/estoque/boms', {
		method: 'POST',
		body: JSON.stringify(paraPayloadBom(payload))
	}));
}

export async function atualizarBom(
	id: string,
	payload: CreateBomPayload
): Promise<BomResponse> {
	return mapBom(await stockFetch<BomResponse>(`/estoque/boms/${id}`, {
		method: 'PUT',
		body: JSON.stringify(paraPayloadBom(payload))
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
				idItem: Number(i.idItem),
				quantidadeConsumida: Number(i.quantidade)
			}))
		})
	});
}
