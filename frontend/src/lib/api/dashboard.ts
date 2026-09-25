import { get } from 'svelte/store';
import { apiFetch } from './client';
import { auth } from '$lib/stores/auth';
import type { DashboardSummary, UnreadCount } from '$lib/types/dashboard';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function fetchSummary(fetchFn: typeof fetch = fetch): Promise<DashboardSummary> {
	return await apiFetch<DashboardSummary>('/dashboard/summary', { headers: bearer() }, fetchFn);
}

export async function fetchUnreadCount(fetchFn: typeof fetch = fetch): Promise<UnreadCount> {
	return await apiFetch<UnreadCount>(
		'/notifications/unread/count',
		{ headers: bearer() },
		fetchFn
	);
}

export async function completeTask(id: string): Promise<void> {
	await apiFetch<void>(`/tasks/${id}`, {
		method: 'PATCH',
		headers: { ...bearer() },
		body: JSON.stringify({ done: true })
	});
}