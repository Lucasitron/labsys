import type { Meta } from '$lib/utils/stock-status';
import type {
	LancamentoStatus,
	TipoLancamento,
	FechamentoStatus,
	CompraStatus,
	TipoDoacao
} from '$lib/types/financeiro';

export function lancamentoStatusMeta(status: LancamentoStatus): Meta {
	switch (status) {
		case 'Pendente':
			return { label: 'Pendente', color: 'warn' };
		case 'Pago':
			return { label: 'Pago', color: 'success' };
		case 'Atrasado':
			return { label: 'Atrasado', color: 'danger' };
		case 'Cancelado':
			return { label: 'Cancelado', color: 'muted' };
	}
}

export function tipoMeta(tipo: TipoLancamento): Meta {
	switch (tipo) {
		case 'Entrada':
			return { label: 'Entrada', color: 'success' };
		case 'Saída':
			return { label: 'Saída', color: 'danger' };
	}
}

export function fechamentoStatusMeta(status: FechamentoStatus): Meta {
	switch (status) {
		case 'Aberta':
			return { label: 'Aberta', color: 'brand' };
		case 'Concluída':
			return { label: 'Concluída', color: 'success' };
		case 'Cancelada':
			return { label: 'Cancelada', color: 'muted' };
	}
}

export function compraStatusMeta(status: CompraStatus): Meta {
	switch (status) {
		case 'Registrada':
			return { label: 'Registrada', color: 'brand' };
		case 'Visualizada':
			return { label: 'Visualizada', color: 'warn' };
		case 'Concluída':
			return { label: 'Concluída', color: 'success' };
	}
}

export function doacaoTipoMeta(tipo: TipoDoacao): Meta {
	switch (tipo) {
		case 'Doação':
			return { label: 'Doação', color: 'brand' };
		case 'Projeto':
			return { label: 'Projeto', color: 'success' };
	}
}
