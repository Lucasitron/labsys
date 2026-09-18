import { get } from 'svelte/store';
import { goto } from '$app/navigation';
import { apiFetch, ApiError } from './client';
import { auth, logout } from '$lib/stores/auth';
import type { DashboardSummary, UnreadCount } from '$lib/types/dashboard';

const NOT_AUTH_REDIRECT = '/auth/login?redirect=%2Fdashboard';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

async function handle401(err: unknown): Promise<never> {
	if (err instanceof ApiError && err.status === 401) {
		logout();
		await goto(NOT_AUTH_REDIRECT);
	}
	throw err;
}

export async function fetchSummary(fetchFn: typeof fetch = fetch): Promise<DashboardSummary> {
	try {
		return await apiFetch<DashboardSummary>(
			'/dashboard/summary',
			{ headers: bearer() },
			fetchFn
		);
	} catch (err) {
		return handle401(err);
	}
}

export async function fetchUnreadCount(fetchFn: typeof fetch = fetch): Promise<UnreadCount> {
	try {
		return await apiFetch<UnreadCount>(
			'/notifications/unread/count',
			{ headers: bearer() },
			fetchFn
		);
	} catch (err) {
		return handle401(err);
	}
}

export async function completeTask(id: string): Promise<void> {
	try {
		await apiFetch<void>(`/tasks/${id}`, {
			method: 'PATCH',
			headers: { ...bearer() },
			body: JSON.stringify({ done: true })
		});
	} catch (err) {
		await handle401(err);
	}
}