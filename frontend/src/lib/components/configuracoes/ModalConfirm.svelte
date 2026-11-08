<script lang="ts">
	import Modal from '$lib/components/ui/Modal.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import type { Tone } from '$lib/types/stock';

	interface Props {
		open: boolean;
		titulo: string;
		mensagem: string;
		confirmLabel?: string;
		cancelLabel?: string;
		tone?: Tone;
		loading?: boolean;
		onConfirm: () => void;
		onClose: () => void;
	}

	let {
		open,
		titulo,
		mensagem,
		confirmLabel = 'Confirmar',
		cancelLabel = 'Cancelar',
		tone = 'danger',
		loading = false,
		onConfirm,
		onClose
	}: Props = $props();

	const TONES: Record<Tone, { icon: 'warning' | 'info'; wrap: string; button: string }> = {
		danger: {
			icon: 'warning',
			wrap: 'bg-danger/10 text-danger',
			button: 'bg-danger hover:bg-danger/80'
		},
		warn: {
			icon: 'warning',
			wrap: 'bg-warn/10 text-warn',
			button: 'bg-warn hover:bg-warn/80'
		},
		success: {
			icon: 'info',
			wrap: 'bg-success/10 text-success',
			button: 'bg-success hover:bg-success/80'
		},
		brand: {
			icon: 'info',
			wrap: 'bg-brand/10 text-brandhi',
			button: 'bg-brand hover:bg-brandhi'
		},
		muted: {
			icon: 'info',
			wrap: 'bg-muted/10 text-muted',
			button: 'bg-muted hover:bg-muted/80'
		},
		ink: {
			icon: 'info',
			wrap: 'bg-ink/10 text-ink',
			button: 'bg-ink hover:bg-ink/80'
		}
	};

	const estilo = $derived(TONES[tone]);
</script>

<div data-testid="cfg-confirm">
	<Modal {open} title={titulo} onClose={!loading ? onClose : undefined} width="sm">
		{#snippet children()}
			<div class="flex items-start gap-3">
				<span
					class="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-full {estilo.wrap}"
				>
					<Icon name={estilo.icon} class="h-5 w-5" />
				</span>
				<p class="text-sm leading-relaxed text-muted">{mensagem}</p>
			</div>
		{/snippet}

		{#snippet footer()}
			<button
				type="button"
				data-testid="cfg-confirm-cancel"
				onclick={onClose}
				disabled={loading}
				class="rounded-lg border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition-colors hover:bg-border/40 disabled:cursor-not-allowed disabled:opacity-50"
			>
				{cancelLabel}
			</button>
			<button
				type="button"
				data-testid="cfg-confirm-accept"
				onclick={onConfirm}
				disabled={loading}
				class="rounded-lg px-4 py-2 text-sm font-semibold text-white transition-colors disabled:cursor-not-allowed disabled:opacity-50 {estilo.button}"
			>
				{loading ? 'Processando…' : confirmLabel}
			</button>
		{/snippet}
	</Modal>
</div>