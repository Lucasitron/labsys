import type {
	Cartao5S,
	ChamadoStatus,
	GravidadePendencia,
	InspecaoStatus,
	KanbanColuna,
	MaquinaStatus,
	MesaAuditoriaResultado,
	PenalidadeStatus,
	PenalidadeTipo,
	PreventivaStatus,
	ProjetoStatus,
	SituacaoMesas,
	SituacaoPendencia,
	TarefaStatus,
	Tone
} from '$lib/types/producao';

export interface Meta {
	label: string;
	color: Tone;
}

export const PROJETO_STATUS_META: Record<ProjetoStatus, Meta> = {
	Planejado: { label: 'Planejado', color: 'muted' },
	'Em andamento': { label: 'Em andamento', color: 'brand' },
	Concluído: { label: 'Concluído', color: 'success' },
	Cancelado: { label: 'Cancelado', color: 'muted' }
};

export const TAREFA_STATUS_META: Record<TarefaStatus, Meta> = {
	Pendente: { label: 'Pendente', color: 'warn' },
	'Em andamento': { label: 'Em andamento', color: 'brand' },
	Concluída: { label: 'Concluída', color: 'success' },
	Atrasada: { label: 'Atrasada', color: 'danger' },
	Bloqueada: { label: 'Bloqueada', color: 'muted' }
};

export const KANBAN_COLUNA_META: Record<KanbanColuna, Meta> = {
	Fila: { label: 'Fila', color: 'muted' },
	Produção: { label: 'Produção', color: 'brand' },
	Acabamento: { label: 'Acabamento', color: 'warn' },
	Pronto: { label: 'Pronto', color: 'success' },
	Entregue: { label: 'Entregue', color: 'muted' }
};

export const MAQUINA_STATUS_META: Record<MaquinaStatus, Meta> = {
	Disponível: { label: 'Disponível', color: 'success' },
	'Em manutenção': { label: 'Em manutenção', color: 'warn' },
	Suspensa: { label: 'Suspensa', color: 'danger' }
};

export const CHAMADO_STATUS_META: Record<ChamadoStatus, Meta> = {
	Aberto: { label: 'Aberto', color: 'brand' },
	Atribuído: { label: 'Atribuído', color: 'brand' },
	'Em andamento': { label: 'Em andamento', color: 'warn' },
	'Aguardando compra': { label: 'Aguardando compra', color: 'warn' },
	Resolvido: { label: 'Resolvido', color: 'success' }
};

export const PREVENTIVA_STATUS_META: Record<PreventivaStatus, Meta> = {
	Programada: { label: 'Programada', color: 'brand' },
	Concluída: { label: 'Concluída', color: 'success' },
	Atrasada: { label: 'Atrasada', color: 'danger' }
};

export const INSPECAO_STATUS_META: Record<InspecaoStatus, Meta> = {
	Programada: { label: 'Programada', color: 'brand' },
	Concluída: { label: 'Concluída', color: 'success' },
	Atrasada: { label: 'Atrasada', color: 'danger' }
};

export const CARTAO_META: Record<Cartao5S, Meta> = {
	Verde: { label: 'Verde', color: 'success' },
	Amarelo: { label: 'Amarelo', color: 'warn' },
	Vermelho: { label: 'Vermelho', color: 'danger' }
};

export const GRAVIDADE_META: Record<GravidadePendencia, Meta> = {
	Leve: { label: 'Leve', color: 'warn' },
	Moderada: { label: 'Moderada', color: 'brand' },
	Grave: { label: 'Grave', color: 'danger' }
};

export const PENDENCIA_STATUS_META: Record<SituacaoPendencia, Meta> = {
	Aberta: { label: 'Aberta', color: 'warn' },
	Resolvida: { label: 'Resolvida', color: 'success' }
};

export const PENALIDADE_TIPO_META: Record<PenalidadeTipo, Meta> = {
	Aviso: { label: 'Aviso', color: 'muted' },
	Advertência: { label: 'Advertência', color: 'warn' },
	Suspensão: { label: 'Suspensão', color: 'danger' },
	Expulsão: { label: 'Expulsão', color: 'danger' }
};

export const PENALIDADE_STATUS_META: Record<PenalidadeStatus, Meta> = {
	Ativa: { label: 'Ativa', color: 'danger' },
	Cumprida: { label: 'Cumprida', color: 'success' }
};

export const MESA_STATUS_META: Record<SituacaoMesas, Meta> = {
	Aprovada: { label: 'Aprovada', color: 'success' },
	Pendente: { label: 'Pendente', color: 'warn' }
};

export const MESA_AUDITORIA_META: Record<MesaAuditoriaResultado, Meta> = {
	ATIVO: { label: 'ATIVO', color: 'success' },
	ABANDONADO: { label: 'ABANDONADO', color: 'danger' }
};

export function gravidadeMeta(gravidade: GravidadePendencia): Meta {
	return GRAVIDADE_META[gravidade];
}

export function notaToCartao(nota: number): Cartao5S {
	if (nota >= 90) return 'Verde';
	if (nota < 70) return 'Vermelho';
	return 'Amarelo';
}

export function slugTestId(label: string): string {
	return label
		.toLowerCase()
		.normalize('NFD')
		.replace(/[\u0300-\u036f]/g, '')
		.replace(/[^a-z0-9]+/g, '-')
		.replace(/^-+|-+$/g, '');
}