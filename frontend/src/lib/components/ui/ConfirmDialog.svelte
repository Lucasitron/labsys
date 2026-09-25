<script lang="ts">
	import Modal from './Modal.svelte';
	import Icon, { type IconName } from './Icon.svelte';

	interface Props {
		open: boolean;
		title: string;
		message: string;
		confirmLabel?: string;
		cancelLabel?: string;
		danger?: boolean;
		loading?: boolean;
		onCancel: () => void;
		onConfirm: () => void;
		icon?: IconName | null;
	}

	let {
		open,
		title,
		message,
		confirmLabel = 'Confirmar',
		cancelLabel = 'Cancelar',
		danger = true,
		loading = false,
		onCancel,
		onConfirm,
		icon = 'warning'
	}: Props = $props();
</script>

<Modal {open} {title} onClose={!loading ? onCancel : undefined} width="sm">
	{#snippet children()}
		<div class="flex items-start gap-3">
			<span
				class="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-full {danger
					? 'bg-danger/10 text-danger'
					: 'bg-brand/10 text-brandhi'}"
			>
				<Icon name={icon ?? 'warning'} class="h-5 w-5" />
			</span>
			<p class="text-sm leading-relaxed text-muted">{message}</p>
		</div>
	{/snippet}

	{#snippet footer()}
		<button
			onclick={onCancel}
			disabled={loading}
			class="rounded-lg border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition-colors hover:bg-border/40 disabled:cursor-not-allowed disabled:opacity-50"
		>
			{cancelLabel}
		</button>
		<button
			onclick={onConfirm}
			disabled={loading}
			class="rounded-lg px-4 py-2 text-sm font-semibold text-white transition-colors disabled:cursor-not-allowed disabled:opacity-50 {danger
				? 'bg-danger hover:bg-danger/80'
				: 'bg-brand hover:bg-brandhi'}"
		>
			{loading ? 'Processando…' : confirmLabel}
		</button>
	{/snippet}
</Modal>