import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type { ConvidarUsuarioPayload, Nivel, NivelMatrix, NivelMembro } from '$lib/types/rh';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export const NIVEL_PARA_INT: Record<Nivel, number> = {
	admin: 0,
	bolsista: 1,
	voluntario: 2,
	estagiario: 3,
	recrutando: 4
};

const INT_PARA_NIVEL: Nivel[] = ['admin', 'bolsista', 'voluntario', 'estagiario', 'recrutando'];

interface RawNivel {
	id: string;
	codigo: number;
	label: string;
	total: number;
}

interface RawPermissao {
	id: string;
	rotulo: string;
	admin: string;
	bolsista: string;
	voluntario: string;
	estagiario: string;
	recrutando: string;
}

// Adaptador de leitura: o back serve {niveis[], permissoes[]} —
// converte p/ o Record da tela (sem inventar vínculo).
export function mapNivelMatrix(raw: { niveis: RawNivel[]; permissoes: RawPermissao[] }): NivelMatrix {
	const niveis = raw.niveis
		.map((n) => INT_PARA_NIVEL[n.codigo] ?? null)
		.filter((n): n is Nivel => n !== null);
	const matriz: NivelMatrix['matriz'] = {};
	const modulos: string[] = [];
	for (const p of raw.permissoes ?? []) {
		modulos.push(p.id);
		matriz[p.id] = {
			admin: p.admin,
			bolsista: p.bolsista,
			voluntario: p.voluntario,
			estagiario: p.estagiario,
			recrutando: p.recrutando
		};
	}
	return { modulos, niveis, matriz };
}

export async function getNiveis(fetchFn: typeof fetch = fetch): Promise<NivelMatrix> {
	const raw = await apiFetch<{ niveis: RawNivel[]; permissoes: RawPermissao[] }>(
		'/rh/niveis',
		{ headers: bearer() },
		fetchFn
	);
	return mapNivelMatrix(raw);
}

export async function alterarNivel(
	membroId: string,
	nivel: Nivel,
	fetchFn: typeof fetch = fetch
): Promise<NivelMembro> {
	return await apiFetch<NivelMembro>(
		`/rh/niveis/${membroId}/membros`,
		{ method: 'PATCH', headers: bearer(), body: JSON.stringify({ nivel: NIVEL_PARA_INT[nivel] }) },
		fetchFn
	);
}

export async function convidarUsuario(
	payload: ConvidarUsuarioPayload,
	fetchFn: typeof fetch = fetch
): Promise<NivelMembro> {
	if (!payload.matricula?.trim()) {
		throw new Error('Informe a matrícula para o convite.');
	}
	const body: Record<string, unknown> = {
		nomeCompleto: payload.name,
		matricula: payload.matricula.trim()
	};
	if (payload.nivel) body['nivelAcesso'] = NIVEL_PARA_INT[payload.nivel];
	return await apiFetch<NivelMembro>(
		'/rh/niveis/convites',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(body) },
		fetchFn
	);
}
