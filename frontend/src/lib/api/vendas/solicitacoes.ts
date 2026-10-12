import { bearer } from '$lib/api/bearer';
import { ApiError, apiFetch } from '$lib/api/client';
import type {
	CreateSolicitacaoPayload,
	DecidirSolicitacaoPayload,
	ListSolicitacoesParams,
	SolicitacaoAlvoTipo,
	SolicitacaoEdicao,
	SolicitacaoStatus,
	SolicitacoesResult,
	TipoSolicitacao
} from '$lib/types/vendas';


const TIPO_PARA_UPPER: Record<TipoSolicitacao, string> = {
	'Alteração de dados': 'ALTERACAO_DADOS',
	'Mudança de status': 'MUDANCA_STATUS',
	'Mover encomenda': 'MOVER_ENCOMENDA',
	Outra: 'OUTRA'
};

const UPPER_PARA_TIPO: Record<string, TipoSolicitacao> = {
	ALTERACAO_DADOS: 'Alteração de dados',
	MUDANCA_STATUS: 'Mudança de status',
	MOVER_ENCOMENDA: 'Mover encomenda',
	OUTRA: 'Outra'
};

const ALVO_PARA_TIPO: Record<SolicitacaoAlvoTipo, string> = {
	CLI: 'CLIENTE',
	OC: 'ORCAMENTO',
	EN: 'ENCOMENDA'
};

const TIPO_PARA_ALVO: Record<string, SolicitacaoAlvoTipo> = {
	CLIENTE: 'CLI',
	ORCAMENTO: 'OC',
	ENCOMENDA: 'EN'
};

export interface RawSolicitacao {
	id: number | string;
	tipo: string;
	alvoTipo: string;
	alvoId: number | string;
	campo: string;
	valorAtual?: string | null;
	valorProposto: string;
	justificativa: string;
	status: SolicitacaoStatus;
	solicitanteId: number | string;
	decididoPor?: number | string | null;
	motivoDecisao?: string | null;
	dataCriacao: string;
	dataDecisao?: string | null;
}

export function mapSolicitacao(raw: RawSolicitacao): SolicitacaoEdicao {
	return {
		id: String(raw.id),
		tipo: UPPER_PARA_TIPO[raw.tipo] ?? 'Outra',
		alvo: {
			tipo: TIPO_PARA_ALVO[raw.alvoTipo] ?? 'CLI',
			id: String(raw.alvoId),
			nome: ''
		},
		campo: raw.campo,
		valorAtual: raw.valorAtual ?? undefined,
		valorProposto: raw.valorProposto,
		justificativa: raw.justificativa,
		status: raw.status,
		solicitante: String(raw.solicitanteId),
		motivo: raw.motivoDecisao ?? undefined,
		decididoPor: raw.decididoPor != null ? String(raw.decididoPor) : undefined,
		decididoEm: raw.dataDecisao ?? undefined,
		createdBy: String(raw.solicitanteId)
	};
}

export async function listSolicitacoes(
	params: ListSolicitacoesParams = {},
	fetchFn: typeof fetch = fetch
): Promise<SolicitacoesResult> {
	const query = new URLSearchParams();
	if (params.status) query.set('status', params.status);
	const qs = query.toString();
	const res = await apiFetch<{ solicitacoes: RawSolicitacao[]; counts: Record<string, number> }>(
		`/vendas/solicitacoes${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
	return { solicitacoes: res.solicitacoes.map(mapSolicitacao), counts: res.counts };
}

export async function createSolicitacao(
	payload: CreateSolicitacaoPayload,
	fetchFn: typeof fetch = fetch
): Promise<SolicitacaoEdicao> {
	const body: Record<string, unknown> = {
		tipo: TIPO_PARA_UPPER[payload.tipo],
		alvoTipo: ALVO_PARA_TIPO[payload.alvo.tipo],
		alvoId: Number(payload.alvo.id),
		valorProposto: payload.valorProposto,
		justificativa: payload.justificativa
	};
	if (payload.campo !== undefined) body['campo'] = payload.campo;
	if (payload.valorAtual !== undefined) body['valorAtual'] = payload.valorAtual;
	const raw = await apiFetch<RawSolicitacao>(
		'/vendas/solicitacoes',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(body) },
		fetchFn
	);
	return mapSolicitacao(raw);
}

export async function decidirSolicitacao(
	id: string,
	decisao: DecidirSolicitacaoPayload,
	fetchFn: typeof fetch = fetch
): Promise<SolicitacaoEdicao> {
	if (!decisao.aprovada && !decisao.motivo?.trim()) {
		throw new ApiError(422, 'MOTIVO_OBRIGATORIO', 'Informe o motivo da rejeição.');
	}
	const body: Record<string, unknown> = {
		decisao: decisao.aprovada ? 'APROVAR' : 'REJEITAR'
	};
	if (decisao.motivo?.trim()) body['motivo'] = decisao.motivo.trim();
	const raw = await apiFetch<RawSolicitacao>(
		`/vendas/solicitacoes/${id}/decisao`,
		{ method: 'PUT', headers: bearer(), body: JSON.stringify(body) },
		fetchFn
	);
	return mapSolicitacao(raw);
}
