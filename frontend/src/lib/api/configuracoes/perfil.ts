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
