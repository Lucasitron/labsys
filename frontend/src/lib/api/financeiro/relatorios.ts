import { bearer } from '$lib/api/bearer';
import { apiFetch } from '$lib/api/client';
import type {
	FluxoCaixa,
	Dre,
	Lucratividade,
	Inadimplencia,
	DoacoesDespesas,
	CustoMaquina,
	RelatorioPeriodoParams
} from '$lib/types/financeiro';

const BASE = '/financeiro/relatorios';


function periodoQuery(params: RelatorioPeriodoParams = {}): string {
	const query = new URLSearchParams();
	if (params.periodo) query.set('periodo', params.periodo);
	const qs = query.toString();
	return qs ? `?${qs}` : '';
}

interface RawSemana {
	semana: string;
	entradas: number | string;
	saidas: number | string;
	liquido: number | string;
}

export async function getFluxoCaixa(
	params: RelatorioPeriodoParams = {},
	fetchFn: typeof fetch = fetch
): Promise<FluxoCaixa> {
	const raw = await apiFetch<{
		entradas: number | string;
		saidas: number | string;
		saldo: number | string;
		semanas: RawSemana[];
	}>(`${BASE}/fluxo-caixa${periodoQuery(params)}`, { headers: bearer() }, fetchFn);
	return {
		linhas: (raw.semanas ?? []).map((s) => ({
			periodo: s.semana,
			entradas: Number(s.entradas),
			saidas: Number(s.saidas),
			liquido: Number(s.liquido)
		})),
		total: Number(raw.saldo)
	};
}

export async function getDre(
	params: RelatorioPeriodoParams = {},
	fetchFn: typeof fetch = fetch
): Promise<Dre> {
	const raw = await apiFetch<Record<string, number | string>>(
		`${BASE}/dre${periodoQuery(params)}`,
		{ headers: bearer() },
		fetchFn
	);
	return {
		receitaOperacional: Number(raw['receitaOperacional'] ?? 0),
		custosDiretos: Number(raw['custosDiretos'] ?? 0),
		despesasOperacionais: Number(raw['despesasOperacionais'] ?? 0),
		doacoesRecursos: Number(raw['doacoesRecursos'] ?? 0),
		resultado: Number(raw['resultado'] ?? 0)
	};
}

interface RawLucratividade {
	idEncomenda: number | string;
	valorVenda: number | string;
	custoTotal: number | string;
	margem: number | string;
	margemPercentual: number | string;
}

export async function getLucratividade(
	_params: RelatorioPeriodoParams = {},
	fetchFn: typeof fetch = fetch
): Promise<Lucratividade> {
	const raw = await apiFetch<RawLucratividade[]>(
		`${BASE}/lucratividade`,
		{ headers: bearer() },
		fetchFn
	);
	return {
		linhas: (raw ?? []).map((l) => ({
			idEncomenda: String(l.idEncomenda),
			valorVenda: Number(l.valorVenda),
			custoTotal: Number(l.custoTotal),
			margemReais: Number(l.margem),
			margemPercentual: Number(l.margemPercentual)
		}))
	};
}

interface RawInadimplencia {
	idLancamento: number | string;
	referencia?: string | null;
	valor: number | string;
	dataVencimento: string;
	diasEmAtraso: number | string;
}

export async function getInadimplencia(
	_params: RelatorioPeriodoParams = {},
	fetchFn: typeof fetch = fetch
): Promise<Inadimplencia> {
	const raw = await apiFetch<RawInadimplencia[]>(
		`${BASE}/inadimplencia`,
		{ headers: bearer() },
		fetchFn
	);
	return {
		linhas: (raw ?? []).map((l) => ({
			// O back não serve cliente (só id/referência) — sem inventar nome.
			cliente: `#${l.idLancamento}`,
			referencia: l.referencia ?? '',
			vencimento: l.dataVencimento,
			diasAtraso: Number(l.diasEmAtraso),
			valor: Number(l.valor)
		}))
	};
}

interface RawMes {
	mes: string;
	doacoes: number | string;
	despesas: number | string;
	saldo: number | string;
}

export async function getDoacoesDespesas(
	params: RelatorioPeriodoParams = {},
	fetchFn: typeof fetch = fetch
): Promise<DoacoesDespesas> {
	const raw = await apiFetch<{ saldo: number | string; meses: RawMes[] }>(
		`${BASE}/doacoes-despesas${periodoQuery(params)}`,
		{ headers: bearer() },
		fetchFn
	);
	return {
		linhas: (raw.meses ?? []).map((m) => ({
			mes: m.mes,
			doacoes: Number(m.doacoes),
			despesas: Number(m.despesas),
			saldo: Number(m.saldo)
		})),
		acumulado: Number(raw.saldo)
	};
}

interface RawCustoMaquina {
	idMaquina: number | string;
	custo: number | string;
	percentualTotal: number | string;
}

export async function getCustoMaquina(
	_params: RelatorioPeriodoParams = {},
	fetchFn: typeof fetch = fetch
): Promise<CustoMaquina> {
	const raw = await apiFetch<RawCustoMaquina[]>(
		`${BASE}/custo-maquina`,
		{ headers: bearer() },
		fetchFn
	);
	const linhas = (raw ?? []).map((l) => ({
		// O back não serve horas por máquina (só custo) — sem inventar.
		maquina: String(l.idMaquina),
		horas: 0,
		custo: Number(l.custo),
		percentual: Number(l.percentualTotal)
	}));
	return { linhas, total: linhas.reduce((soma, l) => soma + l.custo, 0) };
}
