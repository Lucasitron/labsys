import { fetchMe } from '$lib/api/auth';
import type { Perfil } from '$lib/types/configuracoes';

// GET /configuracoes/perfil não existe no back: o perfil é montado de /auth/me.
export async function getPerfil(fetchFn: typeof fetch = fetch): Promise<Perfil> {
	const usuario = await fetchMe(fetchFn);
	return {
		nome: usuario.name,
		email: usuario.email ?? '',
		telefone: null,
		funcao: null,
		avatar: null
	};
}

// PUT /configuracoes/perfil não existe no back: edição gateada na tela
// (sem contrato inventado).
export function updatePerfil(): Promise<Perfil> {
	return Promise.reject(
		new Error('Edição de perfil indisponível: nenhum endpoint de atualização no momento.')
	);
}
