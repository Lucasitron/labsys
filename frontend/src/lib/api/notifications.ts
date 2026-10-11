import { get } from 'svelte/store';
import { apiFetch } from './client';
import { auth } from '$lib/stores/auth';
import type {
	HistoryEntry,
	HistoryFilters,
	HistoryPayload,
	Notification,
	NotificationPreferences,
	NotificationType,
	PagedNotifications
} from '$lib/types/notifications';

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

interface RawNotification {
	id: number | string;
	type: NotificationType;
	title: string;
	body: string;
	read: boolean;
	createdAt: string;
	link?: string | null;
	channel?: string;
}

function mapNotification(raw: RawNotification): Notification {
	return {
		id: String(raw.id),
		type: raw.type,
		title: raw.title,
		body: raw.body,
		read: raw.read,
		createdAt: raw.createdAt,
		link: raw.link ?? undefined
	};
}

function unwrapItems<T>(items: T[] | { data: T[] }): T[] {
	return Array.isArray(items) ? items : (items?.data ?? []);
}

export async function listNotifications(
	params: ListNotificationsParams = {},
	fetchFn: typeof fetch = fetch
): Promise<PagedNotifications<Notification>> {
	const query = new URLSearchParams();
	if (params.page !== undefined) query.set('page', String(params.page));
	if (params.pageSize !== undefined) query.set('size', String(params.pageSize));
	if (params.type !== undefined) query.set('type', params.type);
	if (params.read !== undefined) query.set('read', String(params.read));
	const qs = query.toString();
	const res = await apiFetch<{
		items: RawNotification[] | { data: RawNotification[] };
		total: number;
		page: number;
		size?: number;
		pageSize?: number;
		totalPages: number;
	}>(
		`/notificacoes${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
	return {
		items: unwrapItems(res.items).map(mapNotification),
		total: res.total,
		page: res.page,
		pageSize: res.size ?? res.pageSize ?? 10,
		totalPages: res.totalPages
	};
}

export async function markRead(id: string, fetchFn: typeof fetch = fetch): Promise<void> {
	await apiFetch<void>(`/notification/notificacoes/${id}/ler`, {
		method: 'PATCH',
		headers: bearer()
	}, fetchFn);
}

export async function markAllRead(fetchFn: typeof fetch = fetch): Promise<void> {
	await apiFetch<void>('/notification/notificacoes/ler-todas', {
		method: 'POST',
		headers: bearer()
	}, fetchFn);
}

export async function getPreferences(fetchFn: typeof fetch = fetch): Promise<NotificationPreferences> {
	return await apiFetch<NotificationPreferences>(
		'/notification/preferencias',
		{ headers: bearer() },
		fetchFn
	);
}

export async function savePreferences(
	preferences: NotificationPreferences,
	fetchFn: typeof fetch = fetch
): Promise<void> {
	await apiFetch<void>('/notification/preferencias', {
		method: 'PUT',
		headers: bearer(),
		body: JSON.stringify(preferences)
	}, fetchFn);
}

export async function history(
	filters: HistoryFilters = {},
	fetchFn: typeof fetch = fetch
): Promise<HistoryPayload> {
	const query = new URLSearchParams();
	if (filters.search !== undefined) query.set('search', filters.search);
	if (filters.type !== undefined) query.set('type', filters.type);
	if (filters.read !== undefined) query.set('read', String(filters.read));
	if (filters.channel !== undefined) query.set('channel', filters.channel);
	if (filters.period !== undefined) query.set('period', filters.period);
	const qs = query.toString();
	const res = await apiFetch<{
		items: (RawNotification & { channel?: string })[] | { data: (RawNotification & { channel?: string })[] };
		total: number;
	}>(
		`/notification/historico${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
	const items: HistoryEntry[] = unwrapItems(res.items).map((raw) => ({
		id: String(raw.id),
		title: raw.title,
		type: raw.type,
		recipient: '',
		channel: (raw.channel ?? 'inapp') as HistoryEntry['channel'],
		read: raw.read,
		sentAt: raw.createdAt
	}));
	return { items, total: res.total };
}
