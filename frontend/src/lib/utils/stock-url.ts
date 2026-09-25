export function strParam(searchParams: URLSearchParams, key: string): string {
	return searchParams.get(key)?.trim() ?? '';
}

export function intParam(
	searchParams: URLSearchParams,
	key: string,
	fallback: number,
	min = 1,
	max = 10_000
): number {
	const value = Number(searchParams.get(key));
	if (!Number.isInteger(value)) return fallback;
	return Math.min(max, Math.max(min, value));
}

export function arrParam(searchParams: URLSearchParams, key: string): string[] {
	return searchParams.getAll(key).filter(Boolean);
}

export function boolParam(searchParams: URLSearchParams, key: string): boolean {
	return searchParams.get(key) === '1' || searchParams.get(key) === 'true';
}

export function sanitizeSort(value: string, allowed: string[]): string {
	return allowed.includes(value) ? value : '';
}