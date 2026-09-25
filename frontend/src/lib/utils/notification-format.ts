import type { IconName } from '$lib/components/ui/Icon.svelte';
import type { NotificationChannel, NotificationType, Tone } from '$lib/types/notifications';

export const TONE_STYLES: Record<Tone, string> = {
	brand: 'bg-brand/15 border-brand/30 text-brand',
	warn: 'bg-warn/15 border-warn/30 text-warn',
	success: 'bg-success/15 border-success/30 text-success',
	danger: 'bg-danger/15 border-danger/30 text-danger',
	ink: 'bg-ink/15 border-ink/30 text-ink',
	muted: 'bg-muted/15 border-muted/30 text-muted'
};

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