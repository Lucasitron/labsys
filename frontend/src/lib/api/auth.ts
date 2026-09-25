import { get } from 'svelte/store';
import { apiFetch } from './client';
import { auth } from '$lib/stores/auth';
import type { LoginRequest, LoginResponse, User } from '$lib/types/auth';

export function login(username: string, password: string): Promise<LoginResponse> {
	const body: LoginRequest = { username, password };

	return apiFetch<LoginResponse>('/auth/login', {
		method: 'POST',
		body: JSON.stringify(body)
	});
}

export function fetchMe(fetchFn: typeof fetch = fetch): Promise<User> {
	const { token } = get(auth);

	return apiFetch<User>(
		'/auth/me',
		{ headers: { Authorization: `Bearer ${token}` } },
		fetchFn
	);
}