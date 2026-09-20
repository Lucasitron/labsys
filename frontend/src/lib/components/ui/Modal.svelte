<script lang="ts">
	import type { Snippet } from 'svelte';
	import Icon from './Icon.svelte';

	interface Props {
		open: boolean;
		title: string;
		subtitle?: string;
		onClose?: () => void;
		children?: Snippet;
		footer?: Snippet;
		width?: 'sm' | 'md' | 'lg';
	}

	let {
		open,
		title,
		subtitle = '',
		onClose,
		children,
		footer,
		width = 'md'
	}: Props = $props();

	const widths = { sm: 'max-w-md', md: 'max-w-lg', lg: 'max-w-2xl' };
</script>

{#if open}
	<div class="fixed inset-0 z-50 flex items-center justify-center p-4">
		<div
			class="absolute inset-0 bg-black/60 backdrop-blur-sm"
			onclick={onClose}
			aria-hidden="true"
		></div>
		<div
			role="dialog"
			aria-modal="true"
			aria-label={title}
			class="relative w-full {widths[width]} overflow-hidden rounded-2xl border border-border bg-surface shadow-2xl shadow-black/50"
		>
			<div class="flex items-start justify-between gap-4 border-b border-border px-5 py-4">
				<div>
					<h2 class="text-base font-semibold text-ink">{title}</h2>
					{#if subtitle}
						<p class="mt-0.5 text-sm text-muted">{subtitle}</p>
					{/if}
				</div>
				{#if onClose}
					<button
						onclick={onClose}
						class="rounded-md p-1 text-muted transition-colors hover:bg-border/50 hover:text-ink"
						aria-label="Fechar"
					>
						<Icon name="x-mark" class="h-4 w-4" />
					</button>
				{/if}
			</div>

			<div class="max-h-[70vh] overflow-y-auto px-5 py-4">
				{@render children?.()}
			</div>

			{#if footer}
				<div class="flex flex-wrap items-center justify-end gap-2 border-t border-border bg-elevated/50 px-5 py-4">
					{@render footer()}
				</div>
			{/if}
		</div>
	</div>
{/if}