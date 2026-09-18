import { apiFetch } from './client';
import type { LoginRequest, LoginResponse } from '$lib/types/auth';

export function login(username: string, password: string): Promise<LoginResponse> {
	const body: LoginRequest = { username, password };

	return apiFetch<LoginResponse>('/auth/login', {
		method: 'POST',
		body: JSON.stringify(body)
	});
}