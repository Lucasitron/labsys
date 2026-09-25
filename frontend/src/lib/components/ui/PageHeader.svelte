<script lang="ts">
	import type { Snippet } from 'svelte';

	interface Props {
		title: string;
		subtitle?: string;
		backHref?: string;
		children?: Snippet;
	}

	let { title, subtitle = '', backHref, children }: Props = $props();
</script>

<div class="flex flex-wrap items-center justify-between gap-3">
	<div class="min-w-0">
		{#if backHref}
			<a
				href={backHref}
				class="mb-1 inline-flex items-center gap-1 text-xs font-medium text-muted transition-colors hover:text-brandhi"
			>
				Voltar
			</a>
		{/if}
		<h1 class="truncate text-xl font-semibold tracking-tight text-ink">{title}</h1>
		{#if subtitle}
			<p class="mt-0.5 text-sm text-muted">{subtitle}</p>
		{/if}
	</div>
	{#if children}
		<div class="flex flex-wrap items-center gap-2">
			{@render children()}
		</div>
	{/if}
</div>