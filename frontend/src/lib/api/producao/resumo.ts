import type { ResumoProducao } from '$lib/types/producao';
import { apiFetch } from '../client';
import { bearer } from './client';
import { RESUMO_PRODUCAO_MOCK, USE_PRODUCAO_MOCK } from './mocks';

export async function obterResumo(fetchFn: typeof fetch = fetch): Promise<ResumoProducao> {
	if (USE_PRODUCAO_MOCK) return RESUMO_PRODUCAO_MOCK;
	return await apiFetch<ResumoProducao>(
		'/producao/resumo',
		{ headers: bearer() },
		fetchFn
	);
}