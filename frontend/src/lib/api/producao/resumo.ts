import type { ResumoProducao } from '$lib/types/producao';
import { RESUMO_PRODUCAO_MOCK, USE_PRODUCAO_MOCK } from './mocks';

// O back ainda não expõe um resumo de Produção. `GET /dashboard/resumo` existe,
// mas devolve outro shape (DashboardSummary), então NÃO é usado aqui para não
// mascarar contrato. Sem o mock, devolve um resumo neutro (nada de rota 404).
const RESUMO_VAZIO: ResumoProducao = {
	kpis: {
		projetosAtivos: 0,
		tarefasPendentes: 0,
		tarefasAtrasadas: 0,
		maquinas: 0,
		maquinasForaVerde: 0,
		cartoes: { verde: 0, amarelo: 0, vermelho: 0 },
		kanban: { Fila: 0, Produção: 0, Acabamento: 0, Pronto: 0, Entregue: 0 }
	},
	alertas: [],
	rankingTop: [],
	setores: [],
	maquinas: []
};

export async function obterResumo(): Promise<ResumoProducao> {
	return USE_PRODUCAO_MOCK ? RESUMO_PRODUCAO_MOCK : RESUMO_VAZIO;
}
