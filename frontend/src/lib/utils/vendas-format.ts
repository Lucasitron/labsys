export function formatMoneyBRL(value: number): string {
	return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
}

export function formatDateBR(iso: string): string {
	return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(
		new Date(iso)
	);
}

export function maskDocumento(doc: string): string {
	const digitos = doc.replace(/\D/g, '');
	if (digitos.length > 4) return `••••${digitos.slice(-4)}`;
	return '••••';
}

export type CodigoTipo = 'CLI' | 'OC' | 'EN';

export function codigoPrefix(tipo: CodigoTipo, numero: number | string): string {
	const base = String(numero).padStart(4, '0');
	return `${tipo}-${base}`;
}
