import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	FechamentoEncomenda,
	CreateFechamentoPayload,
	CustoEncomenda,
	ValorHoraNivel,
	DefinirValorHoraPayload,
	ParametroOverhead,
	DefinirOverheadPayload
} from '$lib/types/financeiro';

const FECHAMENTOS = '/financeiro/fechamento-encomenda';
const CUSTOS = '/financeiro/custos-encomenda';
const VALORES_HORA = '/financeiro/valores-hora';
const OVERHEAD = '/financeiro/parametros-overhead';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

type Raw = Record<string, number | string | null | undefined>;

function num(v: number | string | null | undefined): number {
	return Number(v ?? 0);
}

const UPPER_PARA_FECHAMENTO: Record<string, FechamentoEncomenda['status']> = {
	ABERTA: 'Aberta',
	CONCLUIDA: 'Concluída',
	CANCELADA: 'Cancelada'
};

export function mapFechamento(raw: Raw): FechamentoEncomenda {
	const status = String(raw['status'] ?? 'ABERTA');
	return {
		id: String(raw['id'] ?? ''),
		idEncomenda: String(raw['idEncomenda'] ?? ''),
		horasEstimadas: num(raw['horasEstimadas']),
		valorFechado: num(raw['valorFechado']),
		dataFechamento: String(raw['dataFechamento'] ?? ''),
		status: UPPER_PARA_FECHAMENTO[status] ?? 'Aberta'
	};
}

export function mapCusto(raw: Raw): CustoEncomenda {
	return {
		custoMateriais: num(raw['custoMateriais']),
		custoMaoObra: num(raw['custoMaoObra']),
		custoOverhead: num(raw['custoOverhead']),
		custoTotal: num(raw['custoTotal']),
		valorVenda: num(raw['valorVenda']),
		margemLucro: num(raw['margemLucro']),
		dataCalculo: String(raw['dataCalculo'] ?? '')
	};
}

export async function listFechamentos(fetchFn: typeof fetch = fetch): Promise<FechamentoEncomenda[]> {
	const raw = await apiFetch<Raw[]>(FECHAMENTOS, { headers: bearer() }, fetchFn);
	return (raw ?? []).map(mapFechamento);
}

export async function createFechamento(
	payload: CreateFechamentoPayload,
	fetchFn: typeof fetch = fetch
): Promise<FechamentoEncomenda> {
	const raw = await apiFetch<Raw>(
		FECHAMENTOS,
		{
			method: 'POST',
			headers: bearer(),
			body: JSON.stringify({
				idEncomenda: Number(payload.idEncomenda),
				horasEstimadas: Number(payload.horasEstimadas),
				valorFechado: Number(payload.valorFechado),
				...(payload.dataFechamento ? { dataFechamento: payload.dataFechamento } : {})
			})
		},
		fetchFn
	);
	return mapFechamento(raw);
}

export async function getCusto(idEncomenda: string, fetchFn: typeof fetch = fetch): Promise<CustoEncomenda> {
	const raw = await apiFetch<Raw>(`${CUSTOS}/${encodeURIComponent(idEncomenda)}`, { headers: bearer() }, fetchFn);
	return mapCusto(raw);
}

export async function listValoresHora(fetchFn: typeof fetch = fetch): Promise<ValorHoraNivel[]> {
	const raw = await apiFetch<Raw[]>(VALORES_HORA, { headers: bearer() }, fetchFn);
	return (raw ?? []).map((v) => ({
		nivelAcesso: Number(v['nivelAcesso'] ?? 0) as ValorHoraNivel['nivelAcesso'],
		valorHora: num(v['valorHora']),
		dataVigencia: String(v['dataVigencia'] ?? '')
	}));
}

export async function definirValorHora(
	payload: DefinirValorHoraPayload,
	fetchFn: typeof fetch = fetch
): Promise<ValorHoraNivel> {
	const raw = await apiFetch<Raw>(
		VALORES_HORA,
		{
			method: 'POST',
			headers: bearer(),
			body: JSON.stringify({
				nivelAcesso: Number(payload.nivelAcesso),
				valorHora: Number(payload.valorHora),
				dataVigencia: payload.dataVigencia
			})
		},
		fetchFn
	);
	return {
		nivelAcesso: Number(raw['nivelAcesso'] ?? payload.nivelAcesso) as ValorHoraNivel['nivelAcesso'],
		valorHora: num(raw['valorHora']),
		dataVigencia: String(raw['dataVigencia'] ?? payload.dataVigencia)
	};
}

export async function definirOverhead(
	payload: DefinirOverheadPayload,
	fetchFn: typeof fetch = fetch
): Promise<ParametroOverhead> {
	const raw = await apiFetch<Raw>(
		OVERHEAD,
		{
			method: 'POST',
			headers: bearer(),
			body: JSON.stringify({
				valorTaxaHora: Number(payload.valorTaxaHora),
				dataVigencia: payload.dataVigencia
			})
		},
		fetchFn
	);
	return {
		valorTaxaHora: num(raw['valorTaxaHora']),
		dataVigencia: String(raw['dataVigencia'] ?? payload.dataVigencia)
	};
}
