import { buildQuery, stockFetch } from './stock/request';

export interface PersonOption {
	id: string;
	name: string;
}

export async function searchPeople(
	search: string,
	fetchFn: typeof fetch = fetch
): Promise<PersonOption[]> {
	const qs = buildQuery({ search });
	return stockFetch<PersonOption[]>(`/rh/people${qs}`, {}, fetchFn);
}