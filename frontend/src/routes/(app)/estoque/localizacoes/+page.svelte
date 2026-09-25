<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { LocalizacaoResp } from '$lib/types/stock';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const loaded = $derived(!!data);
	const canEdit = $derived(data?.canEdit ?? false);
	const localizacoes = $derived<LocalizacaoResp[]>(data?.localizacoes ?? []);
	const error = $derived(data?.error ?? null);
</script>

<svelte:head>
	<title>Localizações — Estoque — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Localizações"
		subtitle="Mapa físico do estoque — armários, prateleiras e caixas"
	>
		{#snippet children()}
			{#if canEdit}
				<a
					data-testid="loc-novo"
					href="/estoque/localizacoes/novo"
					class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi"
				>
					<Icon name="plus" class="h-4 w-4" /> Nova localização
				</a>
			{/if}
		{/snippet}
	</PageHeader>

	{#if !loaded}
		<div class="overflow-hidden rounded-xl border border-border bg-surface">
			<div class="border-b border-border bg-elevated/50 px-4 py-2.5">
				<TableSkeleton rows={1} columns={3} class="!space-y-0" />
			</div>
			<div class="p-4" data-testid="loc-carregando">
				<TableSkeleton rows={6} columns={3} />
			</div>
		</div>
	{:else if error}
		<div data-testid="loc-erro">
			<ErrorBanner
				message="Não foi possível carregar as localizações"
				hint={error}
				onRetry={() => void invalidateAll()}
			/>
		</div>
	{:else if localizacoes.length === 0}
		<div class="rounded-xl border border-border bg-surface" data-testid="loc-vazio">
			<EmptyState
				icon="map-pin"
				title="Ainda não há localizações"
				description="Cadastre um ponto físico de armazenamento para organizar os itens do estoque."
			>
				{#snippet children()}
					{#if canEdit}
						<a
							data-testid="loc-vazio-novo"
							href="/estoque/localizacoes/novo"
							class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-brandhi"
						>
							<Icon name="plus" class="h-4 w-4" /> Nova localização
						</a>
					{/if}
				{/snippet}
			</EmptyState>
		</div>
	{:else}
		<div class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
			{#each localizacoes as localizacao (localizacao.id)}
				<div
					data-testid="loc-card"
					class="rounded-xl border border-border bg-surface p-5 transition-colors hover:border-brand/40"
				>
					<div
						class="mb-3 flex h-10 w-10 items-center justify-center rounded-lg border border-brand/30 bg-brand/10"
					>
						<Icon name="map-pin" class="h-5 w-5 text-brand" />
					</div>
					<p class="font-mono text-xs text-muted">{localizacao.armario}</p>
					<div class="mt-2 space-y-1">
						{#if localizacao.prateleira}
							<p class="text-sm text-ink">
								<span class="text-muted">Prateleira:</span> {localizacao.prateleira}
							</p>
						{/if}
						{#if localizacao.caixa}
							<p class="text-sm text-ink">
								<span class="text-muted">Caixa:</span> {localizacao.caixa}
							</p>
						{/if}
					</div>
					{#if localizacao.descricao}
						<p
							class="mt-3 border-t border-border pt-3 text-xs text-muted"
							data-testid="loc-descricao"
						>
							{localizacao.descricao}
						</p>
					{/if}
				</div>
			{/each}
		</div>
		<p class="text-xs text-muted" data-testid="loc-contagem">
			{localizacoes.length} {localizacoes.length === 1 ? 'localização' : 'localizações'}
		</p>
	{/if}
</div>