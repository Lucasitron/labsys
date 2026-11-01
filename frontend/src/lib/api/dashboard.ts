import { apiFetch } from './client';
import type { DashboardSummary, UnreadCount } from '$lib/types/dashboard';


export async function fetchSummary(fetchFn: typeof fetch = fetch): Promise<DashboardSummary> {
	return await apiFetch<DashboardSummary>('/dashboard/summary', {}, fetchFn);
}

export async function fetchUnreadCount(fetchFn: typeof fetch = fetch): Promise<UnreadCount> {
	return await apiFetch<UnreadCount>(
		'/notificacoes/nao-lidas/contagem',
		{},
		fetchFn
	);
}

export async function completeTask(id: string): Promise<void> {
	await apiFetch<void>(`/tasks/${id}`, {
		method: 'PATCH',
				body: JSON.stringify({ done: true })
	});
}