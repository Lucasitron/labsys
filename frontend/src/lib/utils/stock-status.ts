import type {
	EntryKind,
	ExitReason,
	ItemStatus,
	LoanComputed,
	LoanCondition,
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

export function exitReasonMeta(reason: ExitReason): Meta {
	switch (reason) {
		case 'projeto':
			return { label: 'Uso em projeto', color: 'brand' };
		case 'consumo_interno':
			return { label: 'Consumo interno', color: 'muted' };
		case 'perda':
			return { label: 'Perda', color: 'danger' };
		case 'descarte':
			return { label: 'Descarte', color: 'warn' };
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
export const EXIT_REASONS: ExitReason[] = ['projeto', 'consumo_interno', 'perda', 'descarte'];
export const LOAN_TABS: { key: string; label: string }[] = [
	{ key: 'ativos', label: 'Ativos' },
	{ key: 'atrasados', label: 'Atrasados' },
	{ key: 'historico', label: 'Histórico' }
];