import { apiFetch } from '$lib/api/client';
import type { Nivel, NivelMatrix, NivelMembro } from '$lib/types/rh';


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

interface RawMembro {
	id: number | string;
	name: string;
	matricula: string;
	nivel: string;
	grupo?: string | null;
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

const NIVEIS_VALIDOS = new Set<string>(['admin', 'bolsista', 'voluntario', 'estagiario', 'recrutando']);

function toNivel(value: string): Nivel | null {
	const lower = value.toLowerCase();
	return NIVEIS_VALIDOS.has(lower) ? (lower as Nivel) : null;
}

// Adaptador de leitura: o back serve {niveis[], membros[], permissoes[]} —
// converte p/ o Record da tela (sem inventar vínculo).
export function mapNivelMatrix(raw: {
	niveis: RawNivel[];
	membros?: RawMembro[];
	permissoes: RawPermissao[];
}): NivelMatrix {
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
	const membros: NivelMembro[] = [];
	for (const m of raw.membros ?? []) {
		const nivel = toNivel(m.nivel);
		if (!nivel) continue;
		membros.push({
			id: String(m.id),
			name: m.name,
			matricula: m.matricula,
			nivel,
			grupo: m.grupo ?? undefined
		});
	}
	return { modulos, niveis, matriz, membros };
}

export async function getNiveis(fetchFn: typeof fetch = fetch): Promise<NivelMatrix> {
	const raw = await apiFetch<{ niveis: RawNivel[]; membros?: RawMembro[]; permissoes: RawPermissao[] }>(
		'/rh/niveis',
		{},
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
		{ method: 'PATCH', body: JSON.stringify({ nivel: NIVEL_PARA_INT[nivel] }) },
		fetchFn
	);
}
