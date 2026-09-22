import { get } from 'svelte/store';
import { apiFetch } from './client';
import { fetchUnreadCount } from './dashboard';
import { auth } from '$lib/stores/auth';
import type {
	HistoryFilters,
	Notification,
	NotificationPreferences,
	NotificationType,
	PagedNotifications
} from '$lib/types/notifications';

export { fetchUnreadCount };

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export interface ListNotificationsParams {
	page?: number;
	pageSize?: number;
	type?: NotificationType;
	read?: boolean;
}

export async function listNotifications(
	params: ListNotificationsParams = {},
	fetchFn: typeof fetch = fetch
): Promise<PagedNotifications<Notification>> {
	const query = new URLSearchParams();
	if (params.page !== undefined) query.set('page', String(params.page));
	if (params.pageSize !== undefined) query.set('pageSize', String(params.pageSize));
	if (params.type !== undefined) query.set('type', params.type);
	if (params.read !== undefined) query.set('read', String(params.read));
	const qs = query.toString();
	return await apiFetch<PagedNotifications<Notification>>(
		`/notifications${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
}

export async function markRead(id: string, fetchFn: typeof fetch = fetch): Promise<void> {
	await apiFetch<void>(`/notifications/${id}/read`, {
		method: 'PATCH',
		headers: bearer()
	}, fetchFn);
}

export async function markAllRead(fetchFn: typeof fetch = fetch): Promise<void> {
	await apiFetch<void>('/notifications/read-all', {
		method: 'POST',
		headers: bearer()
	}, fetchFn);
}

export async function getPreferences(fetchFn: typeof fetch = fetch): Promise<NotificationPreferences> {
	return await apiFetch<NotificationPreferences>(
		'/notifications/preferences',
		{ headers: bearer() },
		fetchFn
	);
}

export async function savePreferences(
	preferences: NotificationPreferences,
	fetchFn: typeof fetch = fetch
): Promise<void> {
	await apiFetch<void>('/notifications/preferences', {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(preferences)
	}, fetchFn);
}

export async function history(filters: HistoryFilters = {}, fetchFn: typeof fetch = fetch): Promise<unknown> {
	const query = new URLSearchParams();
	if (filters.search !== undefined) query.set('search', filters.search);
	if (filters.type !== undefined) query.set('type', filters.type);
	if (filters.read !== undefined) query.set('read', String(filters.read));
	if (filters.channel !== undefined) query.set('channel', filters.channel);
	if (filters.period !== undefined) query.set('period', filters.period);
	const qs = query.toString();
	return await apiFetch<unknown>(
		`/notifications/history${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
}