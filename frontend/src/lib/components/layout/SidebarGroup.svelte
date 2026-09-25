<script lang="ts">
	import { page } from '$app/stores';
	import type { MenuItem } from '$lib/config/menu';
	import { ICON_PATHS } from './sidebar-icons';

	interface Props {
		item: MenuItem;
		collapsed: boolean;
	}

	let { item, collapsed }: Props = $props();

	const activePath = $derived($page.url.pathname);
	const childActive = $derived((path: string) => activePath === path || activePath.startsWith(path + '/'));

	const anyActive = $derived(item.children?.some((child) => childActive(child.path)) ?? false);

	let open = $state(false);

	$effect(() => {
		if (anyActive) open = true;
	});
</script>

{#if collapsed}
	<a
		href={item.path}
		data-testid="sidebar-item"
		title={item.label}
		aria-label={item.label}
		class="mx-2 flex items-center justify-center gap-3 rounded-md px-3 py-2 transition
			{anyActive
				? 'border-l-2 border-brand bg-brand/10 text-brand'
				: 'border-l-2 border-transparent text-muted hover:bg-elevated hover:text-ink'}"
	>
		<svg
			class="h-4 w-4 shrink-0"
			fill="none"
			viewBox="0 0 24 24"
			stroke="currentColor"
			stroke-width="2"
			aria-hidden="true"
		>
			<path stroke-linecap="round" stroke-linejoin="round" d={ICON_PATHS[item.icon]} />
		</svg>
	</a>
{:else}
	<button
		type="button"
		data-testid="sidebar-item"
		onclick={() => (open = !open)}
		class="mx-2 flex w-[calc(100%-1rem)] items-center gap-3 rounded-md px-3 py-2 transition
			{anyActive
				? 'border-l-2 border-brand bg-brand/10 text-brand'
				: 'border-l-2 border-transparent text-muted hover:bg-elevated hover:text-ink'}"
		aria-expanded={open}
	>
		<svg
			class="h-4 w-4 shrink-0"
			fill="none"
			viewBox="0 0 24 24"
			stroke="currentColor"
			stroke-width="2"
			aria-hidden="true"
		>
			<path stroke-linecap="round" stroke-linejoin="round" d={ICON_PATHS[item.icon]} />
		</svg>
		<span class="min-w-0 flex-1 truncate text-sm text-left">{item.label}</span>
		<svg
			class="h-3 w-3 shrink-0 transition-transform duration-200 {open ? 'rotate-180' : ''}"
			fill="none"
			viewBox="0 0 24 24"
			stroke="currentColor"
			stroke-width="2.5"
			aria-hidden="true"
		>
			<path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
		</svg>
	</button>
{/if}

{#if open && !collapsed}
	<div class="ml-5 mt-1 space-y-0.5 border-l border-border pl-3">
		{#each item.children ?? [] as child (child.path)}
			<a
				href={child.path}
				data-testid="sidebar-subitem"
				class="block rounded border-l-2 px-3 py-1.5 text-xs transition
					{childActive(child.path)
						? 'border-brand bg-brand/10 font-medium text-brand'
						: 'border-transparent text-muted hover:bg-elevated hover:text-ink'}"
			>
				{child.label}
			</a>
		{/each}
	</div>
{/if}