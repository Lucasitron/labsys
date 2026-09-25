<script lang="ts">
	import type { StatusSlice } from '$lib/types/dashboard';
	import DonutChart from './DonutChart.svelte';

	interface Props {
		slices: StatusSlice[];
		error?: boolean;
	}

	let { slices, error = false }: Props = $props();

	const total = $derived(slices.reduce((sum, slice) => sum + slice.count, 0));
	const activeCount = $derived(slices.find((s) => s.status === 'active')?.count ?? 0);

	const summary = $derived(
		slices.length > 0
			? `Status das máquinas: ${slices.map((s) => `${s.count} ${s.label.toLowerCase()}`).join(', ')}`
			: 'Status das máquinas: nenhuma máquina'
	);
</script>

<section class="relative rounded-xl border border-border bg-surface p-5">
	<span
		class="absolute right-3 top-3 rounded border border-brand/30 bg-brand/10 px-1.5 py-0.5 text-[9px] font-medium text-brand"
	>
		RESP.
	</span>

	<h2 class="mb-4 text-sm font-semibold">Status das máquinas</h2>

	{#if error}
		<div class="flex items-center justify-center py-8">
			<p class="text-xs text-muted">Não foi possível carregar o gráfico.</p>
		</div>
	{:else}
		<DonutChart
			{slices}
			centerValue={`${activeCount}/${total}`}
			centerLabel="Ativas"
			emptyText="Nenhuma máquina cadastrada ainda."
			ariaLabel={summary}
		/>
	{/if}
</section>