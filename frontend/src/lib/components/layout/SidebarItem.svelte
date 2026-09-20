<script lang="ts">
	import type { MenuItem } from '$lib/config/menu';
	import { ICON_PATHS } from './sidebar-icons';

	interface Props {
		item: MenuItem;
		active: boolean;
		collapsed: boolean;
	}

	let { item, active, collapsed }: Props = $props();
</script>

<a
	href={item.path}
	data-testid="sidebar-item"
	title={collapsed ? item.label : undefined}
	aria-label={collapsed ? item.label : undefined}
	class="mx-2 flex items-center gap-3 rounded-md px-3 py-2 transition
		{active
			? 'border-l-2 border-brand bg-brand/10 text-brand'
			: 'border-l-2 border-transparent text-muted hover:bg-elevated hover:text-ink'}
		{collapsed ? 'justify-center' : ''}"
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
	<span class="text-sm truncate" class:hidden={collapsed}>{item.label}</span>
</a>