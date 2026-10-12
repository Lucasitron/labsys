import { bearer } from '$lib/api/bearer';
import { apiFetch } from '$lib/api/client';
import type { Module } from '$lib/types/auth';
import type { Nivel, PermissaoMatriz, PermissaoNivel } from '$lib/types/configuracoes';


const NOME_PARA_NIVEL: Record<string, Nivel> = {
	ADMIN: 0,
	BOLSISTA: 1,
	VOLUNTARIO: 2,
	ESTAGIARIO: 3,
	RECRUTANDO: 4
};

const ROTULO_PARA_VALOR: Record<string, PermissaoNivel> = {
	Visualizar: 'Ver',
	Editar: 'Editar',
	'Sem acesso': 'Nenhum'
};

interface RawCelula {
	modulo: string;
	nivel: string;
	valor: string;
}

export async function obterPermissoes(fetchFn: typeof fetch = fetch): Promise<PermissaoMatriz> {
	const res = await apiFetch<{ matriz: RawCelula[] }>(
		'/permissoes',
		{ headers: bearer() },
		fetchFn
	);
	const matriz = {} as PermissaoMatriz;
	for (const celula of res.matriz ?? []) {
		const modulo = celula.modulo as Module;
		const nivel = NOME_PARA_NIVEL[celula.nivel];
		if (nivel === undefined) continue;
		const valor = ROTULO_PARA_VALOR[celula.valor] ?? 'Nenhum';
		matriz[modulo] ??= {} as Record<Nivel, PermissaoNivel>;
		matriz[modulo][nivel] = valor;
	}
	return matriz;
}

export async function atualizarPermissao(
	modulo: Module,
	nivel: Nivel,
	valor: PermissaoNivel
): Promise<void> {
	await apiFetch<unknown>(`/permissoes/${modulo}/${nivel}`, {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify({ valor })
	});
}
