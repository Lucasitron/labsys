import { bearer } from '$lib/api/bearer';
import { apiFetch } from '$lib/api/client';
import {
	periodoParaDatas,
	STATUS_PARA_UPPER,
	TIPO_PARA_UPPER,
	UPPER_PARA_STATUS,
	UPPER_PARA_TIPO
} from '$lib/utils/financeiro-map';
import type {
	Lancamento,
	LancamentosResult,
	CreateLancamentoPayload,
	RegistrarPagamentoPayload,
	ListLancamentosParams,
	LancamentoStatus,
	TipoLancamento
} from '$lib/types/financeiro';

const BASE = '/financeiro/lancamentos';


export interface RawLancamento {
	id: number | string;
	idCategoria: number | string;
	tipo: string;
	valor: number | string;
	dataVencimento: string;
	dataPagamento?: string | null;
	status: string;
	idReferenciaExterna?: string | null;
	observacao?: string | null;
	codigo?: string;
}

export function mapLancamento(raw: RawLancamento): Lancamento {
	return {
		id: String(raw.id),
		...(raw.codigo ? { codigo: raw.codigo } : {}),
		categoria: { id: String(raw.idCategoria) },
		tipo: (UPPER_PARA_TIPO[raw.tipo] ?? 'Entrada') as TipoLancamento,
		valor: Number(raw.valor),
		dataVencimento: raw.dataVencimento,
		...(raw.dataPagamento ? { dataPagamento: raw.dataPagamento } : {}),
		status: (UPPER_PARA_STATUS[raw.status] ?? 'Pendente') as LancamentoStatus,
		...(raw.idReferenciaExterna ? { idReferenciaExterna: raw.idReferenciaExterna } : {}),
		...(raw.observacao ? { observacao: raw.observacao } : {})
	};
}

export async function listLancamentos(
	params: ListLancamentosParams = {},
	fetchFn: typeof fetch = fetch
): Promise<LancamentosResult> {
	const query = new URLSearchParams();
	if (params.status) {
		const upper = STATUS_PARA_UPPER[params.status as LancamentoStatus];
		if (upper) query.set('status', upper);
	}
	if (params.tipo) {
		const upper = TIPO_PARA_UPPER[params.tipo];
		if (upper) query.set('tipo', upper);
	}
	if (params.categoria) query.set('idCategoria', params.categoria);
	if (params.periodo) {
		const datas = periodoParaDatas(params.periodo);
		if (datas) {
			query.set('dataInicio', datas.dataInicio);
			query.set('dataFim', datas.dataFim);
		}
	}
	const qs = query.toString();
	const res = await apiFetch<{
		lancamentos: RawLancamento[];
		counts?: Partial<Record<string, number>>;
		resumo?: LancamentosResult['resumo'];
		pagination?: LancamentosResult['pagination'];
	}>(
		`${BASE}${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
	const lancamentos = (res.lancamentos ?? []).map(mapLancamento);
	const total = lancamentos.length;
	return {
		lancamentos,
		pagination: res.pagination ?? {
			page: 1,
			pageSize: total,
			totalItems: total,
			totalPages: 1
		},
		counts: (res.counts ?? {}) as LancamentosResult['counts'],
		resumo: res.resumo ?? { entradas: 0, saidas: 0, pendente: 0 }
	};
}

export async function getLancamento(id: string, fetchFn: typeof fetch = fetch): Promise<Lancamento> {
	// Sem rota :id no back (D10) — detalhe via lista/filtro.
	const { lancamentos } = await listLancamentos({}, fetchFn);
	const achado = lancamentos.find((l) => l.id === id);
	if (!achado) throw new Error('Lançamento não encontrado.');
	return achado;
}

export async function createLancamento(
	payload: CreateLancamentoPayload,
	fetchFn: typeof fetch = fetch
): Promise<Lancamento> {
	const body: Record<string, unknown> = {
		idCategoria: Number(payload.idCategoria),
		tipo: TIPO_PARA_UPPER[payload.tipo],
		valor: Number(payload.valor),
		dataVencimento: payload.dataVencimento
	};
	if (payload.idReferenciaExterna) body['idReferenciaExterna'] = payload.idReferenciaExterna;
	if (payload.observacao) body['observacao'] = payload.observacao;
	const raw = await apiFetch<RawLancamento>(
		BASE,
		{ method: 'POST', headers: bearer(), body: JSON.stringify(body) },
		fetchFn
	);
	return mapLancamento(raw);
}

export async function registrarPagamento(
	id: string,
	payload: RegistrarPagamentoPayload,
	fetchFn: typeof fetch = fetch
): Promise<Lancamento> {
	const raw = await apiFetch<RawLancamento>(
		`${BASE}/${encodeURIComponent(id)}/pagamento`,
		{ method: 'PUT', headers: bearer(), body: JSON.stringify(payload) },
		fetchFn
	);
	return mapLancamento(raw);
}
