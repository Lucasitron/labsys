<script lang="ts">
	import type { StatusSlice } from '$lib/types/dashboard';
	import DonutChart from './DonutChart.svelte';

	interface Props {
		slices: StatusSlice[];
		error?: boolean;
		class?: string;
	}

	let { slices, error = false, class: className = '' }: Props = $props();

	const total = $derived(slices.reduce((sum, slice) => sum + slice.count, 0));

	const summary = $derived(
		slices.length > 0
			? `Encomendas por status: ${slices.map((s) => `${s.count} ${s.label.toLowerCase()}`).join(', ')}`
			: 'Encomendas por status: nenhuma encomenda'
	);
</script>

<section class="rounded-xl border border-border bg-surface p-5 {className}">
	<h2 class="mb-4 text-sm font-semibold">Encomendas por status</h2>

	{#if error}
		<div class="flex items-center justify-center py-8">
			<p class="text-xs text-muted">Não foi possível carregar o gráfico.</p>
		</div>
	{:else}
		<DonutChart
			{slices}
			centerValue={String(total)}
			centerLabel="Total"
			emptyText="Nenhuma encomenda cadastrada ainda."
			ariaLabel={summary}
		/>
	{/if}
</section>