import type {
	CategoriaEnum,
	ItemStatus,
	StockItem,
	CreateItemPayload,
	UpdateItemPayload
} from '$lib/types/stock';
import { deriveItemStatus } from '$lib/utils/stock-status';
import { buildQuery, stockFetch, stockFetchBlob } from './request';

const BASE = '/estoque/itens';

export interface ListarItensParams {
	categoria?: CategoriaEnum;
	idLocalizacao?: string;
	baixo?: boolean;
}

function withStatus(item: StockItem): StockItem {
	return { ...item, status: deriveItemStatus(item.quantidadeAtual, item.estoqueMinimo) };
}

export async function listarItens(
	params: ListarItensParams = {},
	fetchFn: typeof fetch = fetch
): Promise<StockItem[]> {
	const qs = buildQuery({
		categoria: params.categoria,
		idLocalizacao: params.idLocalizacao,
		baixo: params.baixo === true ? '1' : undefined
	});
	const raw = await stockFetch<StockItem[]>(`${BASE}${qs}`, {}, fetchFn);
	return raw.map(withStatus);
}

export async function buscarItem(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<StockItem> {
	return withStatus(await stockFetch<StockItem>(`${BASE}/${id}`, {}, fetchFn));
}

export async function criarItem(payload: CreateItemPayload): Promise<StockItem> {
	return withStatus(
		await stockFetch<StockItem>(BASE, {
			method: 'POST',
			body: JSON.stringify(payload)
		})
	);
}

export async function atualizarItem(
	id: string,
	payload: UpdateItemPayload
): Promise<StockItem> {
	return withStatus(
		await stockFetch<StockItem>(`${BASE}/${id}`, {
			method: 'PUT',
			body: JSON.stringify(payload)
		})
	);
}

// Est-003: remover `deleteItem` — backend não expõe DELETE /itens/{id}. (bloco 2 remove o botão)
export function deleteItem(_id: string): Promise<never> {
	return Promise.reject(new Error('DELETE /itens/{id} não disponível no backend (TODO G-9)'));
}

export async function importarCsv(
	file: File,
	fetchFn: typeof fetch = fetch
): Promise<StockItem[]> {
	const form = new FormData();
	form.append('arquivo', file);
	const raw = await stockFetch<StockItem[]>('/estoque/itens/import', {
		method: 'POST',
		body: form
	}, fetchFn);
	return raw.map(withStatus);
}

export async function exportarCsv(fetchFn: typeof fetch = fetch): Promise<void> {
	const blob = await stockFetchBlob('/estoque/itens/export', {}, fetchFn);
	const url = URL.createObjectURL(blob);
	const link = document.createElement('a');
	link.href = url;
	link.download = 'itens.csv';
	link.click();
	URL.revokeObjectURL(url);
}

// ---- Helpers de exibição (sem API) ----

export function fetchStatuses(): { id: ItemStatus; label: string }[] {
	return [
		{ id: 'available', label: 'Disponível' },
		{ id: 'low', label: 'Baixo' },
		{ id: 'out', label: 'Esgotado' },
		{ id: 'loaned', label: 'Emprestado' },
		{ id: 'maintenance', label: 'Manutenção' }
	];
}

// Legacy alias (est-012 migra a tela para `exportarCsv`).
export async function exportItemsCsv(): Promise<void> {
	return exportarCsv();
}