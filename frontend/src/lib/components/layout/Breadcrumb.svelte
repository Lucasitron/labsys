<script lang="ts">
	import { page } from '$app/stores';
	import { crumbsFor } from '$lib/config/menu';

	const crumbs = $derived(crumbsFor($page.url.pathname));
</script>

<nav aria-label="Trilha de navegação" class="flex min-w-0 items-center gap-2 text-sm">
	<a href="/dashboard" class="shrink-0 text-muted transition hover:text-brand">FabLab</a>
	{#each crumbs as crumb}
		<span class="shrink-0 text-muted/50">/</span>
		{#if crumb.current}
			<span class="truncate font-medium text-ink">{crumb.label}</span>
		{:else}
			<a href={crumb.path} class="shrink-0 text-muted transition hover:text-brand">
				{crumb.label}
			</a>
		{/if}
	{/each}
</nav>