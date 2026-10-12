import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	ListMarketplaceParams,
	MarketplaceResult,
	Plataforma,
	RegistrarVendaPayload,
	RegistroMarketplace
} from '$lib/types/vendas';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

// O back (MarketplaceResource) não devolve `encomenda{}` nem `createdBy`: guards + coerção.
export interface RawMarketplace {
	id: number | string;
	idEncomenda?: number | string | null;
	codigo?: string | null;
	plataforma: Plataforma;
	codigoExterno?: string | null;
	clienteNome?: string | null;
	valorBruto: number | string;
	valorTaxa: number | string;
	valorLiquido: number | string;
	dataVenda?: string | null;
	encomenda?: { codigo?: string | null; valor?: number | string | null } | null;
	criadoPor?: number | string | null;
	createdBy?: number | string | null;
}

export interface RawMarketplaceTotais {
	bruto: number | string;
	taxas: number | string;
	liquido: number | string;
}

export function mapRegistroMarketplace(raw: RawMarketplace): RegistroMarketplace {
	return {
		id: String(raw.id),
		plataforma: raw.plataforma,
		codigoExterno: raw.codigoExterno ?? '',
		encomenda: {
			codigo: raw.encomenda?.codigo ?? raw.codigo ?? '',
			valor: Number(raw.encomenda?.valor ?? raw.valorBruto ?? 0)
		},
		clienteNome: raw.clienteNome ?? '',
		valorBruto: Number(raw.valorBruto),
		valorTaxa: Number(raw.valorTaxa),
		valorLiquido: Number(raw.valorLiquido),
		dataVenda: raw.dataVenda ?? '',
		createdBy:
			raw.criadoPor != null
				? String(raw.criadoPor)
				: (raw.createdBy != null ? String(raw.createdBy) : '')
	};
}

export async function listMarketplace(
	params: ListMarketplaceParams = {},
	fetchFn: typeof fetch = fetch
): Promise<MarketplaceResult> {
	const query = new URLSearchParams();
	if (params.plataforma) query.set('plataforma', params.plataforma);
	if (params.page !== undefined) query.set('page', String(params.page));
	if (params.pageSize !== undefined) query.set('pageSize', String(params.pageSize));
	const qs = query.toString();
	const res = await apiFetch<{ registros: RawMarketplace[]; totais: RawMarketplaceTotais }>(
		`/vendas/marketplace${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
	return {
		registros: (res.registros ?? []).map(mapRegistroMarketplace),
		totais: {
			bruto: Number(res.totais?.bruto ?? 0),
			taxas: Number(res.totais?.taxas ?? 0),
			liquido: Number(res.totais?.liquido ?? 0)
		}
	};
}

export async function registrarVenda(
	payload: RegistrarVendaPayload,
	fetchFn: typeof fetch = fetch
): Promise<RegistroMarketplace> {
	const raw = await apiFetch<RawMarketplace>(
		'/vendas/marketplace',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
	return mapRegistroMarketplace(raw);
}
