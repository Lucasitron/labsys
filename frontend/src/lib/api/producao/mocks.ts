import type { ResumoProducao } from '$lib/types/producao';

// TODO contrato 🟡 — remover mock quando gateway expor /api/producao/**
export const USE_PRODUCAO_MOCK = true; // alternar para false para usar a chamada real (apiFetch)

export const RESUMO_PRODUCAO_MOCK: ResumoProducao = {
	kpis: {
		projetosAtivos: 5,
		tarefasPendentes: 12,
		tarefasAtrasadas: 3,
		maquinas: 8,
		maquinasForaVerde: 2,
		cartoes: { verde: 3, amarelo: 2, vermelho: 1 },
		kanban: { Fila: 3, Produção: 5, Acabamento: 2, Pronto: 1, Entregue: 12 }
	},
	alertas: [
		{
			id: 'alerta-pendencia-grave',
			severidade: 'danger',
			titulo: 'Setor Eletrônica — Caio bloqueado por pendência grave',
			descricao: 'Resolva a pendência grave antes de novas atribuições e encomendas.',
			dataGoto: '/producao/5s/pendencias'
		},
		{
			id: 'alerta-preventiva-atrasada',
			severidade: 'danger',
			titulo: 'MN-017 — preventiva atrasada 3 dias',
			descricao: 'A máquina está fora da cartela verde por preventiva vencida.',
			dataGoto: '/producao/maquinas/mn-017'
		},
		{
			id: 'alerta-auditorias-mes',
			severidade: 'warn',
			titulo: '12/24 auditorias realizadas no mês',
			descricao: 'Metade das auditorias do mês ainda não foi concluída.',
			dataGoto: '/producao/5s/auditoria'
		},
		{
			id: 'alerta-top1',
			severidade: 'success',
			titulo: 'Top 1 do Ranking — Luiza 92',
			descricao: '1 semana livre de atribuições (18–24/09).',
			dataGoto: '/producao/5s/ranking'
		}
	],
	rankingTop: [
		{
			mes: '2026-09',
			posicao: 1,
			responsavel: { id: 'p-2', nome: 'Luiza Torres' },
			nota: 92,
			cartao: 'Verde',
			top1: true,
			semanaLivre: true
		},
		{
			mes: '2026-09',
			posicao: 2,
			responsavel: { id: 'p-5', nome: 'Caio Mendes' },
			nota: 89,
			cartao: 'Amarelo',
			top1: false,
			semanaLivre: false
		},
		{
			mes: '2026-09',
			posicao: 3,
			responsavel: { id: 'p-3', nome: 'Ana Duarte' },
			nota: 87,
			cartao: 'Amarelo',
			top1: false,
			semanaLivre: false
		}
	],
	setores: [
		{
			id: 'set-1',
			nome: 'Eletrônica',
			ciclo: 'semanal',
			responsaveis: [
				{ membro: { id: 'p-5', nome: 'Caio Mendes' }, ps: true },
				{ membro: { id: 'p-9', nome: 'Bia Nunes' }, ps: false }
			],
			auditor: { id: 'p-3', nome: 'Ana Duarte' },
			nota: 92,
			cartao: 'Verde',
			ultimaAuditoria: '2026-09-11',
			proximaAuditoria: '2026-09-18'
		},
		{
			id: 'set-2',
			nome: 'Corte laser',
			ciclo: 'semanal',
			responsaveis: [{ membro: { id: 'p-7', nome: 'Pedro Lima' }, ps: true }],
			auditor: { id: 'p-2', nome: 'Luiza Torres' },
			nota: 61,
			cartao: 'Vermelho',
			ultimaAuditoria: '2026-09-10',
			proximaAuditoria: '2026-09-17'
		},
		{
			id: 'set-3',
			nome: 'Impressão 3D',
			ciclo: 'quinzenal',
			responsaveis: [{ membro: { id: 'p-2', nome: 'Luiza Torres' }, ps: true }],
			auditor: { id: 'p-3', nome: 'Ana Duarte' },
			nota: 84,
			cartao: 'Amarelo',
			ultimaAuditoria: '2026-09-05',
			proximaAuditoria: '2026-09-19'
		}
	],
	maquinas: [
		{
			id: 'maq-1',
			codigo: 'CM-1400',
			nome: 'Cortadora a laser',
			categoria: 'Corte e gravação',
			responsavelManutencao: { id: 'p-8', nome: 'Carlos Rocha' },
			status: 'Disponível',
			cartao: 'Verde'
		},
		{
			id: 'maq-2',
			codigo: 'MN-017',
			nome: 'Máquina de precisão',
			categoria: 'Usinagem',
			responsavelManutencao: { id: 'p-8', nome: 'Carlos Rocha' },
			status: 'Em manutenção',
			cartao: 'Vermelho'
		},
		{
			id: 'maq-3',
			codigo: 'IM-220',
			nome: 'Impressora 3D',
			categoria: 'Prototipagem',
			responsavelManutencao: { id: 'p-6', nome: 'Rita Sousa' },
			status: 'Disponível',
			cartao: 'Amarelo'
		}
	]
};