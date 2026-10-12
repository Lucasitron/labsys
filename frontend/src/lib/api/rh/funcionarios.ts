import { bearer } from '$lib/api/bearer';
import { apiFetch } from '$lib/api/client';


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

// Best-effort (Admin-only): mapeia idPessoa → nivelAcesso p/ enriquecer a
// listagem de pessoas (o PessoaResource não serializa o nível do vínculo).
// Não-Admin recebe 403 e degrada para mapa vazio.
export async function nivelPorPessoa(
	fetchFn: typeof fetch = fetch
): Promise<Map<string, string>> {
	try {
		const funcionarios = await listFuncionarios(fetchFn);
		return new Map(funcionarios.map((f) => [f.idPessoa, f.nivelAcesso]));
	} catch {
		return new Map();
	}
}
