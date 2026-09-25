const money = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function fmtMoney(value?: number | null): string {
	if (value === null || value === undefined || Number.isNaN(value)) return '—';
	return money.format(value);
}

export function fmtQty(value: number, unit?: string): string {
	const n = new Intl.NumberFormat('pt-BR', {
		maximumFractionDigits: 2
	}).format(value);
	return unit ? `${n} ${unit}` : n;
}

export function fmtDate(iso?: string | null): string {
	if (!iso) return '—';

	if (/^\d{4}-\d{2}-\d{2}$/.test(iso)) {
		const [y, m, d] = iso.split('-');
		return `${d}/${m}/${y}`;
	}

	const date = new Date(iso);
	if (Number.isNaN(date.getTime())) return iso;
	return new Intl.DateTimeFormat('pt-BR').format(date);
}

export function fmtDateTime(iso?: string | null): string {
	if (!iso) return '—';

	const date = new Date(iso);
	if (Number.isNaN(date.getTime())) return iso;
	return new Intl.DateTimeFormat('pt-BR', {
		day: '2-digit',
		month: '2-digit',
		year: 'numeric',
		hour: '2-digit',
		minute: '2-digit'
	}).format(date);
}

export function toDateInputValue(date = new Date()): string {
	const y = date.getFullYear();
	const m = String(date.getMonth() + 1).padStart(2, '0');
	const d = String(date.getDate()).padStart(2, '0');
	return `${y}-${m}-${d}`;
}

export function daysFromToday(iso: string, now = new Date()): number {
	const target = new Date(`${iso}T00:00:00`);
	const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
	return Math.round((target.getTime() - today.getTime()) / 86_400_000);
}