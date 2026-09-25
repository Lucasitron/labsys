import type {
	CategoriaEnum,
	EntryKind,
	ItemStatus,
	Emprestimo,
	LocalizacaoResp,
	LoanComputed,
	LoanCondition,
	LoanStatus,
	TipoSaida,
	Tone
} from '$lib/types/stock';

export interface Meta {
	label: string;
	color: Tone;
}

export function statusMeta(status: ItemStatus): Meta {
	switch (status) {
		case 'available':
			return { label: 'Disponível', color: 'success' };
		case 'low':
			return { label: 'Baixo', color: 'warn' };
		case 'out':
			return { label: 'Esgotado', color: 'danger' };
		case 'loaned':
			return { label: 'Emprestado', color: 'brand' };
		case 'maintenance':
			return { label: 'Manutenção', color: 'muted' };
	}
}

export function categoriaMeta(categoria: CategoriaEnum): Meta {
	switch (categoria) {
		case 'INSUMO':
			return { label: 'Insumo', color: 'brand' };
		case 'FERRAMENTA':
			return { label: 'Ferramenta', color: 'success' };
		case 'PECA':
			return { label: 'Peça', color: 'warn' };
	}
}

export function entryKindMeta(kind: EntryKind): Meta {
	switch (kind) {
		case 'compra':
			return { label: 'Compra', color: 'brand' };
		case 'doacao':
			return { label: 'Doação', color: 'success' };
		case 'devolucao':
			return { label: 'Devolução', color: 'warn' };
		case 'ajuste':
			return { label: 'Ajuste', color: 'muted' };
	}
}

export function exitReasonMeta(reason: TipoSaida): Meta {
	switch (reason) {
		case 'CONSUMO':
			return { label: 'Consumo interno', color: 'muted' };
		case 'PERDA':
			return { label: 'Perda', color: 'danger' };
		case 'AJUSTE':
			return { label: 'Ajuste', color: 'warn' };
		case 'EMPRESTIMO':
			return { label: 'Empréstimo', color: 'brand' };
	}
}

export function loanComputedMeta(computed: LoanComputed): Meta {
	switch (computed) {
		case 'no_prazo':
			return { label: 'No prazo', color: 'success' };
		case 'vence_hoje':
			return { label: 'Vence hoje', color: 'warn' };
		case 'atrasado':
			return { label: 'Atrasado', color: 'danger' };
	}
}

export function loanStatusMeta(status: LoanStatus): Meta {
	switch (status) {
		case 'ATIVO':
			return { label: 'Ativo', color: 'brand' };
		case 'DEVOLVIDO':
			return { label: 'Devolvido', color: 'muted' };
		case 'ATRASADO':
			return { label: 'Atrasado', color: 'danger' };
	}
}

export function loanComputed(emprestimo: Emprestimo): LoanComputed {
	if (emprestimo.status === 'DEVOLVIDO') return 'no_prazo';
	const hoje = new Date().toISOString().slice(0, 10);
	const prevista = emprestimo.dataDevolucaoPrevista.slice(0, 10);
	if (prevista < hoje) return 'atrasado';
	if (prevista === hoje) return 'vence_hoje';
	return 'no_prazo';
}

export function deriveItemStatus(quantidadeAtual: number, estoqueMinimo: number): ItemStatus {
	if (quantidadeAtual <= 0) return 'out';
	if (quantidadeAtual < estoqueMinimo) return 'low';
	return 'available';
}

export function localizacaoLabel(localizacao: LocalizacaoResp | null): string {
	if (!localizacao) return 'Sem local';
	const parts = [`A: ${localizacao.armario}`];
	if (localizacao.prateleira) parts.push(`P: ${localizacao.prateleira}`);
	if (localizacao.caixa) parts.push(`C: ${localizacao.caixa}`);
	if (localizacao.descricao) parts.push(localizacao.descricao);
	return parts.join(' · ');
}

export function loanConditionMeta(condition: LoanCondition): Meta {
	switch (condition) {
		case 'bom':
			return { label: 'Bom estado', color: 'success' };
		case 'avaria':
			return { label: 'Com avaria', color: 'warn' };
		case 'danificado':
			return { label: 'Danificado', color: 'danger' };
	}
}

export function quantityTone(current: number, minimum: number): Tone {
	if (current <= 0) return 'danger';
	if (current < minimum) return 'warn';
	return 'ink';
}

export function capacityTone(percent: number): Tone {
	if (percent >= 90) return 'danger';
	if (percent >= 65) return 'warn';
	return 'success';
}

export function availabilityTone(status: 'ok' | 'faltam'): Tone {
	return status === 'ok' ? 'success' : 'danger';
}

export const ENTRY_KINDS: EntryKind[] = ['compra', 'doacao', 'devolucao', 'ajuste'];
export const EXIT_REASONS: TipoSaida[] = ['CONSUMO', 'PERDA', 'AJUSTE', 'EMPRESTIMO'];
export const LOAN_TABS: { key: string; label: string }[] = [
	{ key: 'ativos', label: 'Ativos' },
	{ key: 'atrasados', label: 'Atrasados' },
	{ key: 'historico', label: 'Histórico' }
];