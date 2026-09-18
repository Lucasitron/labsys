export function greeting(date = new Date()): string {
	const hour = date.getHours();
	if (hour < 12) return 'Bom dia';
	if (hour < 18) return 'Boa tarde';
	return 'Boa noite';
}

export function firstName(name: string): string {
	const first = name.trim().split(/\s+/)[0];
	return first ?? '';
}

export function initials(name: string): string {
	const parts = name.trim().split(/\s+/).filter(Boolean);
	if (parts.length === 0) return '--';
	if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
	return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

const RELATIVE_UNITS: Array<[Intl.RelativeTimeFormatUnit, number]> = [
	['year', 31_536_000],
	['month', 2_592_000],
	['week', 604_800],
	['day', 86_400],
	['hour', 3_600],
	['minute', 60],
	['second', 1]
];

export function relativeTime(iso: string, now = new Date()): string {
	const rtf = new Intl.RelativeTimeFormat('pt-BR', { numeric: 'auto' });
	const diff = Math.round((new Date(iso).getTime() - now.getTime()) / 1000);
	const abs = Math.abs(diff);

	for (const [unit, seconds] of RELATIVE_UNITS) {
		if (abs >= seconds) {
			return rtf.format(Math.round(diff / seconds), unit);
		}
	}

	return rtf.format(0, 'second');
}

export function formatNumber(value: number): string {
	return new Intl.NumberFormat('pt-BR').format(value);
}