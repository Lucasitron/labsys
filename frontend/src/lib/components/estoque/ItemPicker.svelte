<script lang="ts">
	import { onMount } from 'svelte';
	import type { StockItem } from '$lib/types/stock';
	import { listarItens } from '$lib/api/stock/items';
	import { statusMeta, categoriaMeta } from '$lib/utils/stock-status';
	import { fmtQty } from '$lib/utils/stock-format';
	import StatusBadge from '../ui/StatusBadge.svelte';
	import Icon from '../ui/Icon.svelte';

	interface Props {
		onSelect: (item: StockItem | null) => void;
		onUnit?: (unit: string) => void;
		selected?: StockItem | null;
		placeholder?: string;
		showStock?: boolean;
		disabled?: boolean;
		hint?: string;
	}

	let {
		onSelect,
		onUnit,
		selected = null,
		placeholder = 'Buscar item por código ou nome…',
		showStock = false,
		disabled = false,
		hint = ''
	}: Props = $props();

	let query = $state(selected?.nome ?? '');
	let results = $state<StockItem[]>([]);
	let searching = $state(false);
	let open = $state(false);
	let touched = $state(false);
	let root: HTMLDivElement | undefined = $state();

	// R-9: busca de itens é client-side (sem parâmetro search no backend).
	let cache: StockItem[] = [];

	function normalize(value: string): string {
		return value.toLowerCase().trim();
	}

	function matches(item: StockItem, term: string): boolean {
		return (
			normalize(item.nome).includes(term) ||
			normalize(item.descricao ?? '').includes(term) ||
			normalize(item.unidadeMedida).includes(term)
		);
	}

	onMount(() => {
		if (touched) return;
		const onDoc = (event: MouseEvent): void => {
			if (root && !root.contains(event.target as Node)) open = false;
		};
		const onKey = (event: KeyboardEvent): void => {
			if (event.key === 'Escape') open = false;
		};
		document.addEventListener('mousedown', onDoc);
		document.addEventListener('keydown', onKey);
		return () => {
			document.removeEventListener('mousedown', onDoc);
			document.removeEventListener('keydown', onKey);
		};
	});

	async function search(q: string): Promise<void> {
		searching = true;
		try {
			if (cache.length === 0) {
				cache = await listarItens({}, fetch);
			}
			const term = normalize(q);
			results = term
				? cache.filter((i) => matches(i, term)).slice(0, 8)
				: [];
			open = true;
		} catch {
			results = [];
			open = true;
		} finally {
			searching = false;
		}
	}

	function handleInput(event: Event): void {
		query = (event.currentTarget as HTMLInputElement).value;
		touched = true;
		if (selected) {
			onSelect(null);
			selected = null;
		}
		if (debounce) clearTimeout(debounce);
		if (query.trim().length < 2) {
			results = [];
			return;
		}
		debounce = setTimeout(() => void search(query.trim()), 300);
	}

	function pick(item: StockItem): void {
		selected = item;
		onSelect(item);
		onUnit?.(item.unidadeMedida);
		query = item.nome;
		open = false;
	}

	function clear(): void {
		selected = null;
		onSelect(null);
		query = '';
		results = [];
		open = false;
	}

	const qtyColors: Record<string, string> = {
		success: 'text-success',
		warn: 'text-warn',
		danger: 'text-danger',
		brand: 'text-brandhi',
		muted: 'text-muted',
		ink: 'text-ink'
	};

	function metaByStatus(item: StockItem): ReturnType<typeof statusMeta> {
		return statusMeta(item.status);
	}

	function qtyClass(item: StockItem): string {
		return qtyColors[statusMeta(item.status).color];
	}

	let debounce: ReturnType<typeof setTimeout> | undefined;
</script>

<div bind:this={root} class="relative">
	{#if selected}
		<div
			class="flex items-center justify-between gap-2 rounded-lg border border-border bg-elevated px-3 py-2.5"
		>
			<div class="min-w-0">
				<p class="truncate text-sm font-medium text-ink">{selected.nome}</p>
				<p class="truncate text-xs text-muted">
					{selected.categoria === 'INSUMO'
						? 'Insumo'
						: selected.categoria === 'FERRAMENTA'
							? 'Ferramenta'
							: 'Peça'}
					{#if selected.unidadeMedida} · {selected.unidadeMedida}{/if}
				</p>
			</div>
			<div class="flex shrink-0 items-center gap-2">
				{#if showStock}
					<span class="text-xs text-muted">
						Disponível:
						<span class="font-mono font-semibold {qtyClass(selected)}">
							{fmtQty(selected.quantidadeAtual, selected.unidadeMedida)}
						</span>
						{#if selected.quantidadeAtual < selected.estoqueMinimo && selected.quantidadeAtual > 0}
							<span class="text-muted">
								· mín. {fmtQty(selected.estoqueMinimo, selected.unidadeMedida)}
							</span>
						{/if}
					</span>
				{/if}
				<StatusBadge {...metaByStatus(selected)} />
				{#if !disabled}
					<button
						onclick={clear}
						class="rounded-md p-1 text-muted transition-colors hover:bg-border/50 hover:text-ink"
						aria-label="Remover seleção"
					>
						<Icon name="x-mark" class="h-4 w-4" />
					</button>
				{/if}
			</div>
		</div>
	{:else}
		<div class="relative">
			<span
				class="pointer-events-none absolute inset-y-0 left-3 flex items-center text-muted"
				aria-hidden="true"
			>
				<Icon name="search" class="h-4 w-4" />
			</span>
			<input
				type="search"
				{placeholder}
				bind:value={query}
				oninput={handleInput}
				{disabled}
				class="w-full rounded-lg border border-border bg-elevated py-2.5 pl-9 pr-3 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 disabled:cursor-not-allowed disabled:opacity-50"
			/>
			{#if searching}
				<span
					class="absolute inset-y-0 right-3 flex items-center text-muted"
					aria-hidden="true"
				>
					<span class="h-4 w-4 animate-spin rounded-full border-2 border-border border-t-brand"></span>
				</span>
			{/if}
			{#if hint}
				<p class="mt-1 text-xs text-muted">{hint}</p>
			{/if}
		</div>
	{/if}

	{#if open && results.length > 0}
		<div
			class="absolute left-0 right-0 z-40 mt-1 max-h-60 overflow-y-auto rounded-xl border border-border bg-elevated shadow-xl shadow-black/40"
			role="listbox"
		>
			{#each results as item (item.id)}
				<button
					type="button"
					role="option"
					onclick={() => pick(item)}
					class="flex w-full items-center justify-between gap-3 px-3 py-2.5 text-left transition-colors hover:bg-brand/10"
				>
					<span class="min-w-0">
						<span class="block truncate text-sm text-ink">{item.nome}</span>
						<span class="block truncate text-xs text-muted">{categoriaMeta(item.categoria).label}</span>
					</span>
					<span class="shrink-0 text-xs font-mono text-muted">
						{fmtQty(item.quantidadeAtual, item.unidadeMedida)}
					</span>
				</button>
			{/each}
		</div>
	{:else if open && !searching && query.trim().length >= 2}
		<div
			class="absolute left-0 right-0 z-40 mt-1 rounded-xl border border-border bg-elevated p-4 text-sm text-muted shadow-xl shadow-black/40"
		>
			Nenhum item encontrado.
		</div>
	{/if}
</div>