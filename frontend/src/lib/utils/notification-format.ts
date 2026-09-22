import type { IconName } from '$lib/components/ui/Icon.svelte';
import type { NotificationChannel, NotificationType, Tone } from '$lib/types/notifications';

export interface TypeMeta {
	label: string;
	icon: IconName;
	tone: Tone;
}

export const TYPE_META: Record<NotificationType, TypeMeta> = {
	encomenda: { label: 'Encomenda', icon: 'shopping-bag', tone: 'brand' },
	estoque: { label: 'Estoque', icon: 'cube', tone: 'warn' },
	financeiro: { label: 'Financeiro', icon: 'chart', tone: 'success' },
	producao: { label: 'Produção', icon: 'wrench', tone: 'ink' },
	pessoas: { label: 'Pessoas & RH', icon: 'users', tone: 'brand' },
	sistema: { label: 'Sistema', icon: 'bell', tone: 'muted' }
};

export interface ChannelMeta {
	label: string;
	tone: Tone;
}

export const CHANNEL_BADGE: Record<NotificationChannel, ChannelMeta> = {
	inapp: { label: 'In-app', tone: 'brand' },
	email: { label: 'E-mail', tone: 'success' },
	push: { label: 'Push', tone: 'warn' }
};

export const NOTIFICATION_TYPES: NotificationType[] = [
	'encomenda',
	'estoque',
	'financeiro',
	'producao',
	'pessoas',
	'sistema'
];