import type {
	CreateLoanPayload,
	Loan,
	LoanParams,
	LoansResult,
	PageInfo,
	ReturnLoanPayload
} from '$lib/types/stock';
import { buildQuery, stockFetch } from './request';

interface LoanListResult {
	loans: Loan[];
	pagination: PageInfo;
	counts: { ativos: number; atrasados: number };
}

const STATUS_MAP: Record<string, string> = {
	ativos: 'active',
	atrasados: 'overdue',
	historico: 'history'
};

export async function fetchLoans(
	fetchFn: typeof fetch,
	params: LoanParams
): Promise<LoansResult> {
	const qs = buildQuery({
		status: STATUS_MAP[params.tab] ?? 'active',
		page: params.page,
		pageSize: params.pageSize,
		search: params.search
	});
	const result = await stockFetch<LoanListResult>(`/stock/loans${qs}`, {}, fetchFn);
	return {
		loans: result.loans ?? [],
		pagination: result.pagination,
		counts: result.counts
	};
}

export async function fetchLoan(fetchFn: typeof fetch, id: string): Promise<Loan> {
	return stockFetch<Loan>(`/stock/loans/${id}`, {}, fetchFn);
}

export function createLoan(payload: CreateLoanPayload): Promise<unknown> {
	return stockFetch<unknown>('/stock/loans', {
		method: 'POST',
		body: JSON.stringify(payload)
	});
}

export function returnLoan(id: string, payload: ReturnLoanPayload): Promise<unknown> {
	return stockFetch<unknown>(`/stock/loans/${id}/return`, {
		method: 'POST',
		body: JSON.stringify(payload)
	});
}