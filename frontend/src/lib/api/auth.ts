import { get } from 'svelte/store';
import { apiFetch } from './client';
import { auth } from '$lib/stores/auth';
import type {
	BackendLoginBody,
	BackendRoleName,
	LoginResponse,
	MeResponse,
	RefreshResponse,
	Role,
	User
} from '$lib/types/auth';

const ROLE_NOME_PARA_INT: Record<BackendRoleName, Role> = {
	ADMIN: 0,
	BOLSISTA: 1,
	VOLUNTARIO: 2,
	ESTAGIARIO: 3,
	RECRUTANDO: 4
};

export function mapRoleNameToInt(nome: BackendRoleName): Role {
	return ROLE_NOME_PARA_INT[nome] ?? 4;
}

export function mapMeToUser(me: MeResponse, fallbackRole: Role = 4): User {
	return {
		id: String(me.idUser),
		username: me.nomeUsuario,
		name: me.nomeUsuario,
		role: me.permissions?.find((p) => p.active)?.role ?? fallbackRole,
		email: me.email
	};
}

export function login(username: string, password: string): Promise<LoginResponse> {
	const body: BackendLoginBody = username.includes('@')
		? { email: username, senha: password }
		: { nomeUsuario: username, senha: password };

	return apiFetch<LoginResponse>('/auth/login', {
		method: 'POST',
		body: JSON.stringify(body)
	});
}

export function refresh(refreshToken: string): Promise<RefreshResponse> {
	return apiFetch<RefreshResponse>('/auth/refresh', {
		method: 'POST',
		body: JSON.stringify({ refreshToken })
	});
}

export async function logout(): Promise<void> {
	const { token } = get(auth);

	await apiFetch<{ message: string }>('/auth/logout', {
		method: 'POST',
		headers: token ? { Authorization: `Bearer ${token}` } : {},
		body: token ? JSON.stringify({ token }) : undefined
	});
}

export function fetchMeRaw(token: string, fetchFn: typeof fetch = fetch): Promise<MeResponse> {
	return apiFetch<MeResponse>(
		'/auth/me',
		{ headers: { Authorization: `Bearer ${token}` } },
		fetchFn
	);
}

export async function fetchMe(fetchFn: typeof fetch = fetch): Promise<User> {
	const { token } = get(auth);
	const me = await fetchMeRaw(token ?? '', fetchFn);
	return mapMeToUser(me);
}
