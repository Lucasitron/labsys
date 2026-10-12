import type { ContagemKanban, ResumoProducao } from '$lib/types/producao';
import type { DashboardSummary } from '$lib/types/dashboard';
import { bearer } from '$lib/api/bearer';
import { apiFetch } from '$lib/api/client';

// O gateway não expõe `GET /producao/resumo`; a agregação mora no Dashboard
// (`GET /dashboard/resumo`, mesmo shape de `/dashboard/summary`). Adaptamos o
// que existe e deixamos vazio o que o contrato não fornece (a UI mostra estado
// vazio) — sem mock e sem chamar rota 404.
const KANBAN_POR_STATUS: Record<string, keyof ContagemKanban> = {
	fila: 'Fila',
	producao: 'Produção',
	acabamento: 'Acabamento',
	pronto: 'Pronto',
	entregue: 'Entregue'
};

export function mapResumoProducao(summary: DashboardSummary): ResumoProducao {
	const kanban: ContagemKanban = { Fila: 0, Produção: 0, Acabamento: 0, Pronto: 0, Entregue: 0 };
	for (const fatia of summary.ordersByStatus ?? []) {
		const coluna = KANBAN_POR_STATUS[fatia.status?.toLowerCase()];
		if (coluna) kanban[coluna] = fatia.count;
	}

	const maquinasTotal = summary.kpis.machinesActive?.total ?? 0;
	const maquinasAtivas = summary.kpis.machinesActive?.value ?? 0;

	return {
		kpis: {
			projetosAtivos: summary.kpis.ordersActive?.value ?? 0,
			tarefasPendentes: summary.tasks?.length ?? 0,
			tarefasAtrasadas: (summary.tasks ?? []).filter((t) => t.urgent).length,
			maquinas: maquinasTotal,
			maquinasForaVerde: Math.max(0, maquinasTotal - maquinasAtivas),
			cartoes: { verde: 0, amarelo: 0, vermelho: 0 },
			kanban
		},
		alertas: [],
		rankingTop: [],
		setores: [],
		maquinas: []
	};
}

export async function obterResumo(fetchFn: typeof fetch = fetch): Promise<ResumoProducao> {
	const summary = await apiFetch<DashboardSummary>(
		'/dashboard/resumo',
		{ headers: bearer() },
		fetchFn
	);
	return mapResumoProducao(summary);
}
