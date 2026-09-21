<script lang="ts">
	import { get } from 'svelte/store';
	import { page } from '$app/stores';
	import { menuItems } from '$lib/config/menu';
	import { auth } from '$lib/stores/auth';
	import { canView } from '$lib/utils/permissions';
	import SidebarItem from './SidebarItem.svelte';
	import SidebarGroup from './SidebarGroup.svelte';

	interface Props {
		collapsed: boolean;
		onCollapse: () => void;
		mobileOpen: boolean;
		onCloseMobile: () => void;
	}

	let { collapsed, onCollapse, mobileOpen, onCloseMobile }: Props = $props();

	const visibleItems = $derived(
		menuItems
			.filter((item) => canView(get(auth).user, item.module))
			.map((item) => ({
				...item,
				children: item.children?.filter(
					(child) => !child.canSee || child.canSee(get(auth).user)
				)
			}))
	);
	const activePath = $derived($page.url.pathname);
	const isActive = (path: string) => activePath === path || activePath.startsWith(path + '/');
</script>

{#snippet content(kind: 'desktop' | 'mobile')}
	<div class="flex h-14 items-center gap-3 border-b border-border px-4">
		<div
			class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-brand/30 bg-brand/10"
		>
			<span class="text-xs font-bold tracking-tight text-brand">FL</span>
		</div>
		<span
			class="text-sm font-semibold tracking-tight truncate"
			class:hidden={kind === 'desktop' && collapsed}
		>
			FabLab
		</span>
		{#if kind === 'desktop'}
			<button
				type="button"
				onclick={onCollapse}
				aria-label="Colapsar menu"
				class="ml-auto rounded p-1 text-muted transition hover:bg-elevated hover:text-ink"
			>
				<svg
					class="h-4 w-4 transition-transform duration-200 {collapsed ? 'rotate-180' : ''}"
					fill="none"
					viewBox="0 0 24 24"
					stroke="currentColor"
					stroke-width="2"
					aria-hidden="true"
				>
					<path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
				</svg>
			</button>
		{:else}
			<button
				type="button"
				onclick={onCloseMobile}
				aria-label="Fechar menu"
				class="ml-auto rounded p-1 text-muted transition hover:bg-elevated hover:text-ink"
			>
				<svg
					class="h-5 w-5"
					fill="none"
					viewBox="0 0 24 24"
					stroke="currentColor"
					stroke-width="2"
					aria-hidden="true"
				>
					<path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
				</svg>
			</button>
		{/if}
	</div>

	<nav aria-label="Menu principal" class="flex-1 space-y-0.5 overflow-y-auto py-3">
		{#each visibleItems as item (item.path)}
			{#if item.children && item.children.length > 0}
				<SidebarGroup item={item} collapsed={kind === 'desktop' && collapsed} />
			{:else}
				<SidebarItem
					item={item}
					active={isActive(item.path)}
					collapsed={kind === 'desktop' && collapsed}
				/>
			{/if}
		{/each}
	</nav>

	<div class="border-t border-border px-4 py-3">
		<span
			class="text-[10px] text-muted/60"
			class:hidden={kind === 'desktop' && collapsed}
		>
			v0.1
		</span>
	</div>
{/snippet}

<aside
	class="hidden shrink-0 flex-col bg-surface transition-all duration-200 lg:flex {collapsed
		? 'w-16'
		: 'w-60'}"
>
	{@render content('desktop')}
</aside>

{#if mobileOpen}
<button
	type="button"
	aria-label="Fechar menu"
	class="fixed inset-0 z-40 bg-black/50 lg:hidden"
	onclick={onCloseMobile}
></button>
<div
	class="fixed inset-y-0 left-0 z-50 flex w-60 flex-col bg-surface lg:hidden"
	role="dialog"
	aria-modal="true"
	aria-label="Menu de navegação"
>
	{@render content('mobile')}
</div>
{/if}