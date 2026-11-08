import { formatMoneyBRL } from '$lib/utils/vendas-format';
import type { TipoLancamento } from '$lib/types/financeiro';

export function formatSignedBRL(valor: number, tipo: TipoLancamento): string {
	const sinal = tipo === 'Entrada' ? '+' : '−';
	return `${sinal}${formatMoneyBRL(valor)}`;
}

export function codigoLancamento(numero: number | string): string {
	const base = String(numero).padStart(4, '0');
	return `LAN-${base}`;
}
