<script lang="ts">
	import { toasts, type ToastTone } from '$lib/stores/toast';
	import Icon, { type IconName } from './Icon.svelte';

	const TONES: Record<ToastTone, { icon: IconName; box: string }> = {
		success: { icon: 'check', box: 'bg-success/15 text-success' },
		danger: { icon: 'warning', box: 'bg-danger/15 text-danger' },
		warn: { icon: 'warning', box: 'bg-warn/15 text-warn' },
		info: { icon: 'info', box: 'bg-brand/15 text-brandhi' }
	};
</script>

<div
	class="pointer-events-none fixed bottom-5 left-1/2 z-[60] flex w-full max-w-sm -translate-x-1/2 flex-col items-center gap-2 px-4"
	aria-live="polite"
>
	{#each $toasts as toast (toast.id)}
		<div
			class="pointer-events-auto flex w-full items-start gap-2.5 rounded-xl border border-border bg-elevated px-4 py-3 shadow-xl shadow-black/40"
		>
			<span class="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full {TONES[toast.tone].box}">
				<Icon name={TONES[toast.tone].icon} class="h-3 w-3" />
			</span>
			<p class="flex-1 text-sm text-ink">{toast.message}</p>
			<button
				onclick={() => toasts.dismiss(toast.id)}
				class="shrink-0 text-muted transition-colors hover:text-ink"
				aria-label="Fechar notificação"
			>
				<Icon name="x-mark" class="h-4 w-4" />
			</button>
		</div>
	{/each}
</div>