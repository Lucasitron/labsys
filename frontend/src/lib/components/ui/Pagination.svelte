<script lang="ts">
	import Icon from './Icon.svelte';

	interface Props {
		page: number;
		totalPages: number;
		totalItems?: number;
		onPage: (page: number) => void;
		pageSize?: number;
		pageSizes?: number[];
		onPageSize?: (size: number) => void;
		label?: string;
	}

	let {
		page,
		totalPages,
		totalItems,
		onPage,
		pageSize,
		pageSizes = [10, 25, 50, 100],
		onPageSize,
		label = 'registro(s)'
	}: Props = $props();

	const hasTotals = $derived(totalPages > 1 || (totalItems ?? 0) > 0);
	const prevDisabled = $derived(page <= 1);
	const nextDisabled = $derived(totalPages !== 0 && page >= totalPages);

	const info = $derived.by(() => {
		if (!hasTotals) return '';
		if (totalPages === 0) return `${totalItems ?? 0} ${label}`;
		return `página ${page} de ${totalPages} · ${totalItems ?? ''} ${label}`;
	});
</script>

{#if info}
	<div class="flex flex-wrap items-center justify-between gap-2">
		<p class="text-xs text-muted">{info}</p>
		<div class="flex items-center gap-2">
			{#if pageSize !== undefined && onPageSize}
				<label class="flex items-center gap-1.5 text-xs text-muted">
					<span>Exibir</span>
					<select
						value={pageSize}
						onchange={(e) => onPageSize(Number((e.currentTarget as HTMLSelectElement).value))}
						class="rounded-md border border-border bg-surface px-1.5 py-1 text-xs text-ink focus:border-brand focus:outline-none"
					>
						{#each pageSizes as size (size)}
							<option value={size}>{size}</option>
						{/each}
					</select>
				</label>
			{/if}
			<div class="flex items-center gap-1">
				<button
					onclick={() => !prevDisabled && onPage(page - 1)}
					disabled={prevDisabled}
					class="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-surface text-muted transition-colors hover:border-brand/50 hover:text-brandhi disabled:cursor-not-allowed disabled:opacity-40"
					aria-label="Página anterior"
				>
					<Icon name="chevron-left" class="h-4 w-4" />
				</button>
				<button
					onclick={() => !nextDisabled && onPage(page + 1)}
					disabled={nextDisabled}
					class="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-surface text-muted transition-colors hover:border-brand/50 hover:text-brandhi disabled:cursor-not-allowed disabled:opacity-40"
					aria-label="Próxima página"
				>
					<Icon name="chevron-right" class="h-4 w-4" />
				</button>
			</div>
		</div>
	</div>
{/if}