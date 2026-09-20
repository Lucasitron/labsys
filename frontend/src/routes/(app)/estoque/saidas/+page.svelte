<script lang="ts">
	import type { PageProps } from './$types';
	import MovementList from '$lib/components/estoque/MovementList.svelte';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();
	const canEdit = $derived(data.canEdit ?? false);
	const total = $derived(data.result?.pagination?.totalItems ?? 0);
</script>

<svelte:head>
	<title>Saídas — Estoque — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader title="Saídas" subtitle={total > 0 ? `${total} registros` : ''}>
		{#snippet children()}
			{#if canEdit}
				<a
					href="/estoque/saidas/nova"
					class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi"
				>
					<Icon name="plus" class="h-4 w-4" /> Nova saída
				</a>
			{/if}
		{/snippet}
	</PageHeader>

	<MovementList
		kind="saida"
		path="/estoque/saidas"
		paramKey="reason"
		params={data.params}
		result={data.result}
		error={data.error}
		{canEdit}
	/>
</div>