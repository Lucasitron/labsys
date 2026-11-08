<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import MovementList from '$lib/components/estoque/MovementList.svelte';
	import ItemPicker from '$lib/components/estoque/ItemPicker.svelte';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';

	let { data }: PageProps = $props();

	const canEdit = $derived(data.canEdit ?? false);
	const total = $derived(data.result?.pagination?.totalItems ?? 0);
	const subtitle = $derived(
		data.idItem ? (data.item ? `${total} entrada${total === 1 ? '' : 's'} · ${data.item.nome}` : '') : ''
	);

	function onSelectItem(item: { id: string } | null): void {
		void goto(item ? `/estoque/entradas?idItem=${item.id}` : '/estoque/entradas', {
			invalidateAll: true
		});
	}
</script>

<svelte:head>
	<title>Entradas — Estoque — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader title="Entradas" {subtitle}>
		{#snippet children()}
			{#if canEdit}
				<a
					href="/estoque/entradas/nova"
					class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi"
				>
					<Icon name="plus" class="h-4 w-4" /> Nova entrada
				</a>
			{/if}
		{/snippet}
	</PageHeader>

	<div
		data-testid="ent-aviso"
		class="rounded-xl border border-warn/30 bg-warn/5 px-4 py-3 text-sm text-ink"
	>
		<strong class="font-semibold">R-9 🟡</strong> O contrato atual expõe movimentações somente por
		item — selecione um item abaixo para consultar o histórico de entradas.
	</div>

	<ItemPicker
		selected={data.item}
		onSelect={onSelectItem}
		showStock
		hint="Selecione para filtrar o histórico por item"
	/>

	{#if data.idItem}
		{#if data.error && !data.item}
			<div data-testid="ent-error">
				<ErrorBanner
					message="Não foi possível carregar as entradas"
					hint={data.error}
					onRetry={() => void invalidateAll()}
				/>
			</div>
		{:else}
			<MovementList
				kind="entrada"
				path="/estoque/entradas"
				paramKey="kind"
				params={data.params}
				result={data.result}
				error={data.error}
				{canEdit}
				extraQuery={{ idItem: data.idItem }}
			/>
		{/if}
	{:else}
		<div data-testid="ent-empty">
			<EmptyState
				icon="shopping-bag"
				title="Selecione um item"
				description="Por contrato (R-9), as entradas só podem ser consultadas por item. Escolha um item acima para ver o histórico."
			/>
		</div>
	{/if}
</div>