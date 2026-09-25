<script lang="ts">
	import Icon from '$lib/components/ui/Icon.svelte';
	import type { Notification } from '$lib/types/notifications';
	import { relativeTime } from '$lib/utils/format';
	import { TONE_STYLES, TYPE_META } from '$lib/utils/notification-format';

	interface Props {
		notification: Notification;
		onRead?: (notification: Notification) => void;
	}

	let { notification, onRead }: Props = $props();

	const meta = $derived(TYPE_META[notification.type] ?? TYPE_META.sistema);
	const iconStyle = $derived(TONE_STYLES[meta.tone]);
</script>

<article
	data-testid="notification-row"
	class="flex items-start gap-3 px-4 py-4 transition-colors hover:bg-elevated/40 {notification.read
		? ''
		: 'border-l-2 border-brand bg-brand/5'}"
>
	<span
		class="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border {iconStyle}"
		aria-hidden="true"
	>
		<Icon name={meta.icon} class="h-4 w-4" />
	</span>

	<div class="min-w-0 flex-1">
		<div class="flex flex-wrap items-center gap-2">
			<h3 class="truncate text-sm font-medium text-ink">{notification.title}</h3>
			{#if !notification.read}
				<span
					class="inline-flex items-center gap-1.5 rounded-full border border-brand/30 bg-brand/15 px-2 py-0.5 text-[10px] font-medium text-brand"
				>
					<span class="h-1.5 w-1.5 rounded-full bg-brand" aria-hidden="true"></span>
					Não lida
				</span>
			{/if}
		</div>
		{#if notification.body}
			<p class="mt-1 text-xs leading-relaxed text-muted">{notification.body}</p>
		{/if}
		<p class="mt-1 text-[11px] tabnums text-muted/70">
			{meta.label} · {relativeTime(notification.createdAt)}
		</p>
	</div>

	{#if notification.read}
		<span class="inline-flex shrink-0 items-center gap-1.5 text-[11px] text-muted/60">
			<span class="h-1.5 w-1.5 rounded-full bg-muted/40" aria-hidden="true"></span>
			Lida
		</span>
	{:else}
		<button
			type="button"
			data-testid="notification-mark-read"
			aria-label="Marcar como lida"
			onclick={() => onRead?.(notification)}
			class="flex shrink-0 items-center gap-1.5 rounded-md border border-border bg-elevated px-2.5 py-1.5 text-xs text-muted transition hover:bg-elevated/70 hover:text-ink"
		>
			<Icon name="check" class="h-3.5 w-3.5" />
			Marcar como lida
		</button>
	{/if}
</article>