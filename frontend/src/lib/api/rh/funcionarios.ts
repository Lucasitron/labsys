import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export interface FuncionarioOption {
	id: string;
	idPessoa: string;
	nome: string;
	nivelAcesso: string;
	departamento: string | null;
}

interface RawFuncionario {
	id: number | string;
	idPessoa: number | string;
	nomePessoa: string;
	nivelAcesso: string;
	departamento?: string | null;
}

// GET /rh/funcionarios (Admin) — usa o id do vínculo (não o da pessoa) como
// referência para papéis que exigem funcionário (ex.: idTutor no PS).
export async function listFuncionarios(
	fetchFn: typeof fetch = fetch
): Promise<FuncionarioOption[]> {
	const res = await apiFetch<RawFuncionario[]>('/rh/funcionarios', { headers: bearer() }, fetchFn);
	return (res ?? []).map((f) => ({
		id: String(f.id),
		idPessoa: String(f.idPessoa),
		nome: f.nomePessoa,
		nivelAcesso: f.nivelAcesso,
		departamento: f.departamento ?? null
	}));
}
