import { bearer } from '$lib/api/bearer';
import { apiFetch } from '$lib/api/client';
import type {
	SolicitacaoCompra,
	CreateSolicitacaoCompraPayload,
	ConcluirCompraPayload,
	ConcluirCompraResult,
	CompraStatus
} from '$lib/types/financeiro';

const BASE = '/financeiro/solicitacoes-compra';


const UPPER_PARA_COMPRA: Record<string, CompraStatus> = {
	REGISTRADA: 'Registrada',
	VISUALIZADA: 'Visualizada',
	CONCLUIDA: 'Concluída'
};

export interface RawCompra {
	id: number | string;
	idItemEstoque: number | string;
	quantidade: number | string;
	valorEstimado?: number | string | null;
	status: string;
	dataSolicitacao: string;
}

export function mapCompra(raw: RawCompra): SolicitacaoCompra {
	return {
		id: String(raw.id),
		// O back serve só o id do item (nome resolve via estoque; sem inventar).
		item: `#${raw.idItemEstoque}`,
		quantidade: Number(raw.quantidade),
		valorEstimado: raw.valorEstimado != null ? Number(raw.valorEstimado) : 0,
		status: UPPER_PARA_COMPRA[raw.status] ?? 'Registrada'
	};
}

export async function listSolicitacoesCompra(
	fetchFn: typeof fetch = fetch
): Promise<SolicitacaoCompra[]> {
	const raw = await apiFetch<RawCompra[]>(BASE, { headers: bearer() }, fetchFn);
	return (raw ?? []).map(mapCompra);
}

export async function createSolicitacaoCompra(
	payload: CreateSolicitacaoCompraPayload,
	fetchFn: typeof fetch = fetch
): Promise<SolicitacaoCompra> {
	const idItemEstoque = Number(payload.idItemEstoque ?? payload.item);
	if (!Number.isInteger(idItemEstoque)) {
		throw new Error('Informe o item de estoque da solicitação.');
	}
	const body: Record<string, unknown> = {
		idItemEstoque,
		quantidade: Number(payload.quantidade)
	};
	if (payload.valorEstimado !== undefined) body['valorEstimado'] = Number(payload.valorEstimado);
	const raw = await apiFetch<RawCompra>(
		BASE,
		{ method: 'POST', headers: bearer(), body: JSON.stringify(body) },
		fetchFn
	);
	return mapCompra(raw);
}

export async function concluirCompra(
	id: string,
	_payload?: ConcluirCompraPayload,
	fetchFn: typeof fetch = fetch
): Promise<ConcluirCompraResult> {
	// O back ignora body e só conclui (sem lançamento gerado) — PUT sem corpo.
	const raw = await apiFetch<RawCompra>(
		`${BASE}/${encodeURIComponent(id)}/concluir`,
		{ method: 'PUT', headers: bearer() },
		fetchFn
	);
	return { status: UPPER_PARA_COMPRA[raw.status] ?? 'Concluída' };
}
