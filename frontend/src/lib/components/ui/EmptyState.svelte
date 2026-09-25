<script lang="ts">
	import type { Snippet } from 'svelte';
	import Icon, { type IconName } from './Icon.svelte';

	interface Props {
		icon?: IconName | null;
		title: string;
		description?: string;
		children?: Snippet;
	}

	let { icon = 'box', title, description = '', children }: Props = $props();
</script>

<div class="flex flex-col items-center justify-center gap-2 px-4 py-10 text-center">
	<span class="flex h-12 w-12 items-center justify-center rounded-full bg-muted/10 text-muted">
		<Icon name={icon ?? 'box'} class="h-6 w-6" />
	</span>
	<p class="text-sm font-medium text-ink">{title}</p>
	{#if description}
		<p class="max-w-sm text-sm text-muted">{description}</p>
	{/if}
	{#if children}
		<div class="mt-2">{@render children()}</div>
	{/if}
</div>