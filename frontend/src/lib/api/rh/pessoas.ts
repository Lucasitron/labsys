import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	CreatePessoaPayload,
	Nivel,
	Person,
	PersonDetail,
	PersonStatus,
	PersonType,
	PessoasResult,
	RhListParams,
	UpdatePessoaPayload
} from '$lib/types/rh';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

const STATUS_PARA_UPPER: Record<PersonStatus, string | null> = {
	ativo: 'ATIVO',
	inativo: 'INATIVO',
	afastado: null
};

const NIVEL_PARA_UPPER: Record<Nivel, string> = {
	admin: 'ADMIN',
	bolsista: 'BOLSISTA',
	voluntario: 'VOLUNTARIO',
	estagiario: 'ESTAGIARIO',
	recrutando: 'RECRUTANDO'
};

export interface RawPessoa {
	id: number | string;
	nomeCompleto: string;
	matricula?: string | null;
	dataAdmissao?: string | null;
	contato?: string | null;
	turno?: string | null;
	status?: string | null;
}

function iniciais(nome: string): string {
	const partes = nome.trim().split(/\s+/).filter(Boolean);
	if (partes.length === 0) return '?';
	const primeira = partes[0][0] ?? '?';
	const ultima = partes.length > 1 ? (partes[partes.length - 1][0] ?? '') : '';
	return `${primeira}${ultima}`.toUpperCase();
}

export function mapPerson(raw: RawPessoa): Person {
	const status: PersonStatus =
		raw.status === 'ATIVO' ? 'ativo' : raw.status === 'INATIVO' ? 'inativo' : 'afastado';
	const contato = raw.contato ?? '';
	const ehEmail = contato.includes('@');
	return {
		id: String(raw.id),
		matricula: raw.matricula ?? '',
		name: raw.nomeCompleto,
		type: 'funcionario' as PersonType,
		nivel: 'recrutando' as Nivel,
		status,
		group: null,
		email: ehEmail ? contato : '',
		...(ehEmail ? {} : contato ? { phone: contato } : {}),
		initials: iniciais(raw.nomeCompleto),
		joinedAt: raw.dataAdmissao ?? ''
	};
}

function toPayload(
	payload: CreatePessoaPayload | UpdatePessoaPayload
): Record<string, unknown> {
	const body: Record<string, unknown> = {};
	if (payload.name !== undefined) body['nomeCompleto'] = payload.name;
	if (payload.matricula !== undefined) body['matricula'] = payload.matricula;
	const contato = payload.phone?.trim() || payload.email?.trim();
	if (contato) body['contato'] = contato;
	if (payload.cpf) body['cpf'] = payload.cpf;
	if (payload.dataAdmissao) body['dataAdmissao'] = payload.dataAdmissao;
	if (payload.shift) body['turno'] = payload.shift;
	if (payload.status !== undefined) {
		const upper = STATUS_PARA_UPPER[payload.status];
		if (upper) body['status'] = upper;
	}
	return body;
}

export async function listPessoas(
	params: RhListParams = {},
	fetchFn: typeof fetch = fetch
): Promise<PessoasResult> {
	const query = new URLSearchParams();
	if (params.search) query.set('search', params.search);
	if (params.setor) query.set('setor', params.setor);
	if (params.nivel) {
		const upper = NIVEL_PARA_UPPER[params.nivel as Nivel];
		if (upper) query.set('nivel', upper);
	}
	if (params.status) {
		const upper = STATUS_PARA_UPPER[params.status as PersonStatus];
		if (upper) query.set('status', upper);
	}
	if (params.page !== undefined) query.set('page', String(params.page));
	if (params.pageSize !== undefined) query.set('pageSize', String(params.pageSize));
	const qs = query.toString();
	const res = await apiFetch<{
		pessoas: RawPessoa[];
		paginacao: { page: number; pageSize: number; total: number; totalPages: number };
	}>(
		`/rh/pessoas${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
	let people = (res.pessoas ?? []).map(mapPerson);
	if (params.status) {
		people = people.filter((p) => p.status === params.status);
	}
	return {
		people,
		pagination: {
			page: res.paginacao.page,
			pageSize: res.paginacao.pageSize,
			totalItems: res.paginacao.total,
			totalPages: res.paginacao.totalPages
		}
	};
}

export async function getPessoa(id: string, fetchFn: typeof fetch = fetch): Promise<PersonDetail> {
	const raw = await apiFetch<RawPessoa>(
		`/rh/pessoas/${id}`,
		{ headers: bearer() },
		fetchFn
	);
	return mapPerson(raw);
}

export async function createPessoa(
	payload: CreatePessoaPayload,
	fetchFn: typeof fetch = fetch
): Promise<PersonDetail> {
	const raw = await apiFetch<RawPessoa>(
		'/rh/pessoas',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(toPayload(payload)) },
		fetchFn
	);
	return mapPerson(raw);
}

export async function updatePessoa(
	id: string,
	payload: UpdatePessoaPayload,
	fetchFn: typeof fetch = fetch
): Promise<PersonDetail> {
	const raw = await apiFetch<RawPessoa>(
		`/rh/pessoas/${id}`,
		{ method: 'PUT', headers: bearer(), body: JSON.stringify(toPayload(payload)) },
		fetchFn
	);
	return mapPerson(raw);
}

export async function deletePessoa(id: string, fetchFn: typeof fetch = fetch): Promise<void> {
	await apiFetch<void>(`/rh/pessoas/${id}`, { method: 'DELETE', headers: bearer() }, fetchFn);
}

export { searchPeople } from '$lib/api/rh';
export type { PersonOption } from '$lib/api/rh';
