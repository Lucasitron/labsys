import type {
	CategoriaTipo,
	LancamentoStatus,
	TipoDoacao,
	TipoLancamento
} from '$lib/types/financeiro';

// Mapper único Title (UI) ↔ UPPER (back), aplicado no send e no read.

export const TIPO_PARA_UPPER: Record<TipoLancamento, string> = {
	Entrada: 'ENTRADA',
	Saída: 'SAIDA'
};

export const UPPER_PARA_TIPO: Record<string, TipoLancamento> = {
	ENTRADA: 'Entrada',
	SAIDA: 'Saída'
};

export const STATUS_PARA_UPPER: Record<LancamentoStatus, string> = {
	Pendente: 'PENDENTE',
	Pago: 'PAGO',
	Atrasado: 'ATRASADO',
	Cancelado: 'CANCELADO'
};

export const UPPER_PARA_STATUS: Record<string, LancamentoStatus> = {
	PENDENTE: 'Pendente',
	PAGO: 'Pago',
	ATRASADO: 'Atrasado',
	CANCELADO: 'Cancelado'
};

export const CATEGORIA_TIPO_PARA_UPPER: Record<CategoriaTipo, string> = {
	Receita: 'RECEITA',
	Despesa: 'DESPESA'
};

export const UPPER_PARA_CATEGORIA_TIPO: Record<string, CategoriaTipo> = {
	RECEITA: 'Receita',
	DESPESA: 'Despesa'
};

export const DOACAO_PARA_UPPER: Record<TipoDoacao, string> = {
	Doação: 'DOACAO',
	Projeto: 'PROJETO'
};

export const UPPER_PARA_DOACAO: Record<string, TipoDoacao> = {
	DOACAO: 'Doação',
	PROJETO: 'Projeto'
};

// `periodo` da UI (7d/30d/mes/ano) → {dataInicio, dataFim} do back.
export function periodoParaDatas(periodo: string): { dataInicio: string; dataFim: string } | null {
	const hoje = new Date();
	const fim = hoje.toISOString().slice(0, 10);
	const inicio = new Date(hoje);
	if (periodo === '7d') {
		inicio.setDate(inicio.getDate() - 7);
	} else if (periodo === '30d') {
		inicio.setDate(inicio.getDate() - 30);
	} else if (periodo === 'mes') {
		inicio.setDate(1);
	} else if (periodo === 'ano') {
		inicio.setMonth(0, 1);
	} else {
		return null;
	}
	return { dataInicio: inicio.toISOString().slice(0, 10), dataFim: fim };
}
