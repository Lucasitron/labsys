import type { UnreadCount } from './dashboard';

export type Tone = 'success' | 'warn' | 'danger' | 'brand' | 'muted' | 'ink';

export type NotificationType =
	| 'encomenda'
	| 'estoque'
	| 'financeiro'
	| 'producao'
	| 'pessoas'
	| 'sistema';

export type NotificationChannel = 'inapp' | 'email' | 'push';

export interface Notification {
	id: string;
	type: NotificationType;
	title: string;
	body: string;
	read: boolean;
	createdAt: string;
	link?: string;
}

export interface PagedNotifications<T> {
	items: T[];
	total: number;
	page: number;
	pageSize: number;
	totalPages: number;
}

export type NotificationPreferences = Record<NotificationType, Record<NotificationChannel, boolean>>;

export interface HistoryFilters {
	search?: string;
	type?: NotificationType;
	read?: boolean;
	channel?: NotificationChannel;
	period?: string;
}

export interface HistoryEntry {
	id: string;
	title: string;
	type: NotificationType;
	recipient: string;
	channel: NotificationChannel;
	read: boolean;
	sentAt: string;
}

export interface HistoryPayload {
	items: HistoryEntry[];
	total: number;
}

export type { UnreadCount };