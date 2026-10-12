import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type { ParametroSistema, TokenIntegracao } from '$lib/types/configuracoes';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

interface RawToken {
	id: number | string;
	nome: string;
	prefixo?: string;
	criadoEm?: string | null;
	ultimoUso?: string | null;
	revogado?: boolean;
	// O back devolve `chave` UMA vez no POST; lista nunca inclui.
	chave?: string | null;
}

interface RawSistema {
	identidade: { nomeFablab?: string; nome?: string; logo?: string | null };
	cadenciaChecklist5S: string;
	cadenciaAuditoria5S?: string | null;
	tokens?: RawToken[];
}

export function mapToken(raw: RawToken): TokenIntegracao {
	return {
		id: String(raw.id),
		nome: raw.nome,
		prefixo: raw.prefixo ?? '',
		criadoEm: raw.criadoEm ?? '',
		ultimoUso: raw.ultimoUso ?? null,
		revogado: raw.revogado ?? false
	};
}

export function mapSistema(raw: RawSistema): ParametroSistema {
	return {
		identidade: {
			nome: raw.identidade.nomeFablab ?? raw.identidade.nome ?? '',
			...(raw.identidade.logo !== undefined ? { logo: raw.identidade.logo } : {})
		},
		cadenciaChecklist5S: raw.cadenciaChecklist5S,
		cadenciaAuditoria5S: raw.cadenciaAuditoria5S ?? null,
		tokens: (raw.tokens ?? []).map(mapToken)
	};
}

export async function obterSistema(fetchFn: typeof fetch = fetch): Promise<ParametroSistema> {
	const raw = await apiFetch<RawSistema>(
		'/configuracoes/sistema',
		{ headers: bearer() },
		fetchFn
	);
	return mapSistema(raw);
}

export async function atualizarSistema(
	payload: ParametroSistema,
	fetchFn: typeof fetch = fetch
): Promise<ParametroSistema> {
	const raw = await apiFetch<RawSistema>(
		'/configuracoes/sistema',
		{
			method: 'PUT',
			headers: bearer(),
			body: JSON.stringify({
				identidade: {
					nomeFablab: payload.identidade.nome,
					...(payload.identidade.logo !== undefined ? { logo: payload.identidade.logo } : {})
				},
				cadenciaChecklist5S: payload.cadenciaChecklist5S,
				...(payload.cadenciaAuditoria5S !== undefined
					? { cadenciaAuditoria5S: payload.cadenciaAuditoria5S }
					: {})
			})
		},
		fetchFn
	);
	return mapSistema(raw);
}

export async function listarTokens(fetchFn: typeof fetch = fetch): Promise<TokenIntegracao[]> {
	const raw = await apiFetch<RawToken[]>(
		'/configuracoes/tokens',
		{ headers: bearer() },
		fetchFn
	);
	return (raw ?? []).map(mapToken);
}

export interface TokenCriado extends TokenIntegracao {
	chave?: string;
}

export function criarToken(payload: { nome: string }): Promise<TokenCriado> {
	return apiFetch<RawToken>('/configuracoes/tokens', {
		method: 'POST',
		headers: bearer(),
		body: JSON.stringify(payload)
	}).then((raw) => ({
		...mapToken(raw),
		...(raw.chave ? { chave: raw.chave } : {})
	}));
}

export function revogarToken(id: string): Promise<void> {
	return apiFetch<void>(`/configuracoes/tokens/${id}`, {
		method: 'DELETE',
		headers: bearer()
	});
}
