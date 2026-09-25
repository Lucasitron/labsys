<script lang="ts">
	import type { Snippet } from 'svelte';

	export interface KanbanCard {
		id: string;
		title: string;
		description?: string;
		meta?: string;
	}

	export interface KanbanColumn {
		id: string;
		label: string;
		tone?: 'brand' | 'success' | 'warn' | 'danger' | 'muted';
		count?: number;
		cards: KanbanCard[];
	}

	interface Props {
		columns: KanbanColumn[];
		children?: Snippet<[{ columnId: string; card: KanbanCard }]>;
	}

	let { columns, children }: Props = $props();

	const DOTS: Record<string, string> = {
		success: 'bg-success',
		warn: 'bg-warn',
		danger: 'bg-danger',
		brand: 'bg-brand',
		muted: 'bg-muted'
	};
</script>

<div class="grid auto-cols-[minmax(16rem,1fr)] grid-flow-col gap-4 overflow-x-auto pb-2">
	{#each columns as column (column.id)}
		<section
			aria-label={column.label}
			class="flex flex-col rounded-xl border border-border bg-surface"
		>
			<header class="flex items-center gap-2 border-b border-border px-4 py-3">
				<span
					class="h-2 w-2 shrink-0 rounded-full {DOTS[column.tone ?? 'muted']}"
					aria-hidden="true"
				></span>
				<h3 class="text-sm font-semibold text-ink">{column.label}</h3>
				<span class="ml-auto rounded-full bg-elevated px-2 py-0.5 text-xs text-muted">
					{column.count ?? column.cards.length}
				</span>
			</header>
			<ul class="flex flex-1 flex-col gap-2 p-3">
				{#each column.cards as card (card.id)}
					<li class="rounded-lg border border-border bg-elevated p-3">
						<p class="text-sm font-medium text-ink">{card.title}</p>
						{#if card.description}
							<p class="mt-1 text-xs text-muted">{card.description}</p>
						{/if}
						{#if card.meta}
							<p class="mt-1 font-mono text-xs text-muted">{card.meta}</p>
						{/if}
						{#if children}
							<div class="mt-2 flex flex-wrap gap-2">
								{@render children({ columnId: column.id, card })}
							</div>
						{/if}
					</li>
				{/each}
				{#if column.cards.length === 0}
					<li class="rounded-lg border border-dashed border-border p-3 text-center text-xs text-muted">
						Nenhum item
					</li>
				{/if}
			</ul>
		</section>
	{/each}
</div>
