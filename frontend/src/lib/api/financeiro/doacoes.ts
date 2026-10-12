import { bearer } from '$lib/api/bearer';
import { apiFetch } from '$lib/api/client';
import {
	DOACAO_PARA_UPPER,
	UPPER_PARA_DOACAO,
	periodoParaDatas
} from '$lib/utils/financeiro-map';
import type {
	DoacaoRecurso,
	DoacoesResult,
	CreateDoacaoPayload,
	ListDoacoesParams,
	TipoDoacao
} from '$lib/types/financeiro';

const BASE = '/financeiro/doacoes-recursos';


export interface RawDoacao {
	id: number | string;
	tipo: string;
	origem: string;
	valor: number | string;
	dataRecebimento: string;
	idProjetoAssociado?: number | string | null;
}

export function mapDoacao(raw: RawDoacao): DoacaoRecurso {
	return {
		id: String(raw.id),
		tipo: (UPPER_PARA_DOACAO[raw.tipo] ?? 'Doação') as TipoDoacao,
		origem: raw.origem,
		valor: Number(raw.valor),
		dataRecebimento: raw.dataRecebimento,
		...(raw.idProjetoAssociado != null ? { idProjetoAssociado: String(raw.idProjetoAssociado) } : {})
	};
}

export async function listDoacoes(
	params: ListDoacoesParams = {},
	fetchFn: typeof fetch = fetch
): Promise<DoacoesResult> {
	const query = new URLSearchParams();
	if (params.tipo) {
		const upper = DOACAO_PARA_UPPER[params.tipo as TipoDoacao];
		if (upper) query.set('tipo', upper);
	}
	if (params.periodo) {
		const datas = periodoParaDatas(params.periodo);
		if (datas) {
			query.set('dataInicio', datas.dataInicio);
			query.set('dataFim', datas.dataFim);
		}
	}
	const qs = query.toString();
	const raw = await apiFetch<RawDoacao[]>(
		`${BASE}${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
	const registros = (raw ?? []).map(mapDoacao);
	const ano = String(new Date().getFullYear());
	const doacoes = registros.filter((r) => r.tipo === 'Doação');
	const recebidoAno = doacoes
		.filter((r) => r.dataRecebimento.startsWith(ano))
		.reduce((soma, r) => soma + r.valor, 0);
	return {
		registros,
		resumo: {
			recebidoAno,
			totalDoacoes: doacoes.length,
			totalProjetos: registros.filter((r) => r.tipo === 'Projeto').length,
			mediaDoacao: doacoes.length > 0 ? recebidoAno / doacoes.length : 0
		}
	};
}

export async function createDoacao(
	payload: CreateDoacaoPayload,
	fetchFn: typeof fetch = fetch
): Promise<DoacaoRecurso> {
	const body: Record<string, unknown> = {
		tipo: DOACAO_PARA_UPPER[payload.tipo],
		origem: payload.origem,
		valor: Number(payload.valor),
		dataRecebimento: payload.dataRecebimento
	};
	if (payload.idProjetoAssociado) body['idProjetoAssociado'] = Number(payload.idProjetoAssociado);
	const raw = await apiFetch<RawDoacao>(
		BASE,
		{ method: 'POST', headers: bearer(), body: JSON.stringify(body) },
		fetchFn
	);
	return mapDoacao(raw);
}
