<script lang="ts">
	import { get } from 'svelte/store';
	import { page } from '$app/stores';
	import { menuItems } from '$lib/config/menu';
	import { auth } from '$lib/stores/auth';

	let { children }: { children: import('svelte').Snippet } = $props();

	const producao = $derived(menuItems.find((item) => item.module === 'producao'));
	const activePath = $derived($page.url.pathname);

	const user = $derived(get(auth).user);

	const visibleChildren = $derived(
		(producao?.children ?? []).filter((child) => child.canSee?.(user) ?? true)
	);

	const isActive = (path: string) => activePath === path || activePath.startsWith(path + '/');
</script>

<div data-testid="prd-layout" class="space-y-6">
	<nav aria-label="Seções da Produção" class="flex flex-wrap items-center gap-1.5">
		{#each visibleChildren as child (child.path)}
			<a
				href={child.path}
				data-testid="prd-submenu"
				class="rounded-md border border-border px-3 py-1.5 text-sm transition {isActive(child.path)
					? 'border-brand bg-brand/10 font-medium text-brand'
					: 'text-muted hover:bg-elevated hover:text-ink'}"
			>
				{child.label}
			</a>
		{/each}
	</nav>
	{@render children()}
</div>