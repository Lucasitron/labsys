import type { Meta } from '$lib/utils/stock-status';
import type {
	KanbanStatus,
	OrcamentoStatus,
	Plataforma,
	Prioridade,
	TarefaStatus,
	TipoInteracao
} from '$lib/types/vendas';

export function orcamentoStatusMeta(status: OrcamentoStatus): Meta {
	switch (status) {
		case 'Pendente':
			return { label: 'Pendente', color: 'warn' };
		case 'Aprovado':
			return { label: 'Aprovado', color: 'success' };
		case 'Recusado':
			return { label: 'Recusado', color: 'danger' };
		case 'Ajuste':
			return { label: 'Ajuste', color: 'brand' };
	}
}

export function kanbanStatusMeta(status: KanbanStatus): Meta {
	switch (status) {
		case 'Fila':
			return { label: 'Fila', color: 'muted' };
		case 'Produção':
			return { label: 'Produção', color: 'brand' };
		case 'Acabamento':
			return { label: 'Acabamento', color: 'warn' };
		case 'Pronto':
			return { label: 'Pronto', color: 'success' };
		case 'Entregue':
			return { label: 'Entregue', color: 'muted' };
	}
}

export function plataformaMeta(plataforma: Plataforma): Meta {
	switch (plataforma) {
		case 'Mercado Livre':
			return { label: 'Mercado Livre', color: 'warn' };
		case 'Shopee':
			return { label: 'Shopee', color: 'success' };
		case 'Elo7':
			return { label: 'Elo7', color: 'brand' };
	}
}

export function tarefaStatusMeta(status: TarefaStatus): Meta {
	switch (status) {
		case 'Pendente':
			return { label: 'Pendente', color: 'warn' };
		case 'Em Andamento':
			return { label: 'Em Andamento', color: 'brand' };
		case 'Concluída':
			return { label: 'Concluída', color: 'success' };
	}
}

export function prioridadeMeta(prioridade: Prioridade): Meta {
	switch (prioridade) {
		case 'Alta':
			return { label: 'Alta', color: 'danger' };
		case 'Média':
			return { label: 'Média', color: 'warn' };
		case 'Baixa':
			return { label: 'Baixa', color: 'muted' };
	}
}

export function interacaoTipoMeta(tipo: TipoInteracao): Meta {
	switch (tipo) {
		case 'E-mail':
			return { label: 'E-mail', color: 'brand' };
		case 'Telefone':
			return { label: 'Telefone', color: 'warn' };
		case 'Reunião':
			return { label: 'Reunião', color: 'success' };
		case 'WhatsApp':
			return { label: 'WhatsApp', color: 'success' };
	}
}
