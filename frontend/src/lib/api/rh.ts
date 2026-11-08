import { apiFetch, buildQuery } from './client';

export interface PersonOption {
	id: string;
	name: string;
}

interface RawPessoa {
	id: number | string;
	nomeCompleto: string;
}

export async function searchPeople(
	search: string,
	fetchFn: typeof fetch = fetch
): Promise<PersonOption[]> {
	const qs = buildQuery({ search });
	const res = await apiFetch<{ pessoas: RawPessoa[] }>(`/rh/pessoas${qs}`, {}, fetchFn);
	return (res.pessoas ?? []).map((p) => ({ id: String(p.id), name: p.nomeCompleto }));
}
