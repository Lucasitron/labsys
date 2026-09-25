<script lang="ts">
	import { onMount } from 'svelte';
	import Icon, { type IconName } from './Icon.svelte';

	interface FilterOptionLike {
		id: string;
		label: string;
		count?: number;
	}

	interface Props {
		label: string;
		options: FilterOptionLike[];
		selected: string[];
		onToggle: (id: string) => void;
		onClear?: () => void;
		icon?: IconName | null;
		search?: boolean;
		searchPlaceholder?: string;
		width?: 'xs' | 'sm' | 'md';
		disabled?: boolean;
	}

	let {
		label,
		options,
		selected,
		onToggle,
		onClear,
		icon = 'filter',
		search = true,
		searchPlaceholder = 'Buscar…',
		width = 'sm',
		disabled = false
	}: Props = $props();

	let open = $state(false);
	let query = $state('');

	let root: HTMLDivElement | undefined = $state();

	onMount(() => {
		function onDocClick(event: MouseEvent): void {
			if (root && !root.contains(event.target as Node)) open = false;
		}
		function onKey(event: KeyboardEvent): void {
			if (event.key === 'Escape') open = false;
		}
		document.addEventListener('mousedown', onDocClick);
		document.addEventListener('keydown', onKey);
		return () => {
			document.removeEventListener('mousedown', onDocClick);
			document.removeEventListener('keydown', onKey);
		};
	});

	const visible = $derived(
		query.trim() === ''
			? options
			: options.filter((o) => o.label.toLowerCase().includes(query.toLowerCase()))
	);

	const widths = { xs: 'w-52', sm: 'w-64', md: 'w-80' };
	const selectedCount = $derived(selected.length);
</script>

<div bind:this={root} class="relative">
	<button
		type="button"
		onclick={() => (open = !open)}
		{disabled}
		aria-expanded={open}
		class="btn-filter inline-flex items-center gap-1.5 rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:border-brand/50 hover:text-brandhi disabled:cursor-not-allowed disabled:opacity-50 {selectedCount > 0
			? 'border-brand/50 text-brandhi'
			: ''}"
	>
		<Icon name={icon ?? 'filter'} class="h-4 w-4" />
		<span>{label}</span>
		{#if selectedCount > 0}
			<span
				class="inline-flex h-4 min-w-4 items-center justify-center rounded-full bg-brand px-1 text-[10px] font-bold text-white"
			>{selectedCount}</span
			>
		{/if}
		<Icon
			name={open ? 'chevron-up' : 'chevron-down'}
			class="h-3.5 w-3.5 text-muted"
		/>
	</button>

	{#if open}
		<div
			class="absolute left-0 z-40 mt-1 overflow-hidden rounded-xl border border-border bg-elevated shadow-xl shadow-black/40 {widths[width]}"
		>
			<div class="flex items-center justify-between border-b border-border px-3 py-2">
				<span class="text-xs font-semibold uppercase tracking-wide text-muted">{label}</span>
				{#if selectedCount > 0 && onClear}
					<button
						onclick={() => onClear?.()}
						class="text-xs font-medium text-danger transition-colors hover:text-danger/70"
					>
						Limpar
					</button>
				{/if}
			</div>
			{#if search}
				<div class="border-b border-border p-2">
					<input
						type="search"
						placeholder={searchPlaceholder}
						bind:value={query}
						class="w-full rounded-md border border-border bg-surface px-2.5 py-1.5 text-sm text-ink placeholder:text-muted/70 focus:border-brand focus:outline-none"
					/>
				</div>
			{/if}
			<div class="max-h-56 overflow-y-auto p-1">
				{#if visible.length === 0}
					<p class="px-3 py-4 text-center text-sm text-muted">Nenhuma opção</p>
				{:else}
					{#each visible as opt (opt.id)}
						<label
							class="flex cursor-pointer items-center gap-2 rounded-md px-2.5 py-2 text-sm text-ink transition-colors hover:bg-brand/5"
						>
							<input
								type="checkbox"
								class="h-4 w-4 rounded border-border bg-surface text-brand accent-brand"
								checked={selected.includes(opt.id)}
								onchange={() => onToggle(opt.id)}
							/>
							<span class="flex-1 truncate">{opt.label}</span>
							{#if opt.count !== undefined}
								<span class="text-[10px] font-semibold text-muted">{opt.count}</span>
							{/if}
						</label>
					{/each}
				{/if}
			</div>
		</div>
	{/if}
</div>