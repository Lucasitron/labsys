<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import { fmtDate, fmtQty } from '$lib/utils/stock-format';
	import { exitReasonMeta } from '$lib/utils/stock-status';
	import Icon from '$lib/components/ui/Icon.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';

	let { data }: PageProps = $props();

	const saida = $derived(data.saida);
	const loadError = $derived(data.error);
	const notFound = $derived(data.notFound);

	const motivoMeta = $derived(saida ? exitReasonMeta(saida.tipoSaida) : null);
	const destino = $derived(saida?.idReferencia ?? '');
</script>

<svelte:head>
	<title>Saída — Estoque — FabLab</title>
</svelte:head>

{#if loadError && !saida}
	{#if notFound}
		<div data-testid="saida-empty" class="rounded-xl border border-border bg-surface">
			<div class="flex flex-col items-center justify-center px-6 py-16 text-center">
				<span
					class="mb-4 flex h-12 w-12 items-center justify-center rounded-xl border border-brand/30 bg-brand/10"
				>
					<Icon name="arrow-up-tray" class="h-5 w-5 text-brand" />
				</span>
				<h3 class="mb-1 text-sm font-semibold text-ink">Saída não encontrada</h3>
				<p class="mb-5 max-w-xs text-xs text-muted">
					A saída pode ter sido removida ou o endereço está incorreto. Verifique e tente novamente.
				</p>
				<a
					href="/estoque/saidas"
					class="inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3.5 py-2 text-xs font-medium text-ink transition-colors hover:bg-elevated/70"
				>
					<Icon name="chevron-left" class="h-3.5 w-3.5" /> Voltar para saídas
				</a>
			</div>
		</div>
	{:else}
		<div data-testid="saida-error-retry">
			<ErrorBanner
				message="Não foi possível carregar a saída"
				hint={loadError}
				onRetry={() => void invalidateAll()}
			/>
		</div>
	{/if}
{:else if !saida}
	<div data-testid="saida-loading" class="space-y-4">
		<div class="rounded-xl border border-border bg-surface p-6">
			<Skeleton class="h-5 w-40" />
			<Skeleton class="mt-3 h-8 w-64" />
			<Skeleton class="mt-2 h-3 w-72 opacity-60" />
		</div>
		<div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
			{#each [0, 1, 2, 3] as i (i)}
				<div class="rounded-xl border border-border bg-surface p-4">
					<Skeleton class="h-3 w-20" />
					<Skeleton class="mt-2 h-6 w-16" />
					<Skeleton class="mt-2 h-2 w-24 opacity-60" />
				</div>
			{/each}
		</div>
		<div class="rounded-xl border border-border bg-surface p-6">
			<Skeleton class="h-3 w-36" />
			<Skeleton class="mt-4 h-10 w-full" />
			<Skeleton class="mt-2 h-10 w-full" />
		</div>
	</div>
{:else}
	<div data-testid="saida-detalhe" class="space-y-4">
		<div class="flex items-center justify-between gap-4">
			<a
				data-testid="back-lista"
				href="/estoque/saidas"
				class="inline-flex items-center gap-2 text-xs font-medium text-muted transition-colors hover:text-ink"
			>
				<Icon name="chevron-left" class="h-3.5 w-3.5" /> Voltar para saídas
			</a>
			{#if motivoMeta}
				<StatusBadge label={motivoMeta.label} color={motivoMeta.color} />
			{/if}
		</div>

		<!-- Hero -->
		<section class="rounded-xl border border-border bg-surface p-6">
			<div class="flex flex-wrap items-center gap-5">
				<div
					class="flex h-24 w-24 shrink-0 items-center justify-center rounded-xl border border-border bg-elevated"
				>
					<Icon name="arrow-up-tray" class="h-10 w-10 text-danger" />
				</div>
				<div class="min-w-0 flex-1">
					<p class="font-mono text-xs text-muted">SAIDA-{saida.id}</p>
					<h1 class="mt-0.5 text-2xl font-semibold tracking-tight text-ink">Saída de estoque</h1>
					<p class="mt-1 text-sm text-muted">
						<span class="font-mono text-brand">{saida.idItem}</span>
						{saida.nomeItem ? ` · ${saida.nomeItem}` : ''}
						<span class="mx-1.5 text-muted/50">·</span> {fmtDate(saida.dataSaida)}
					</p>
					<div class="mt-3 flex items-baseline gap-2">
						<span class="text-2xl font-semibold tracking-tight text-danger">
							−{fmtQty(saida.quantidade)}
						</span>
						{#if destino}
							<span class="text-xs text-muted">· destino: {destino}</span>
						{/if}
					</div>
				</div>
			</div>
		</section>

		<!-- Metadata -->
		<div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
			<div class="rounded-xl border border-border bg-surface p-4">
				<span class="text-xs text-muted">Data da saída</span>
				<p class="mt-1 font-mono text-sm font-medium text-ink">{fmtDate(saida.dataSaida)}</p>
			</div>
			<div class="rounded-xl border border-border bg-surface p-4">
				<span class="text-xs text-muted">Motivo</span>
				<p class="mt-1 text-sm font-medium text-ink">{motivoMeta?.label ?? '—'}</p>
			</div>
			<div class="rounded-xl border border-border bg-surface p-4">
				<span class="text-xs text-muted">Destino</span>
				<p class="mt-1 truncate font-mono text-sm font-medium text-ink">{destino || '—'}</p>
			</div>
			<div class="rounded-xl border border-border bg-surface p-4">
				<span class="text-xs text-muted">Observações</span>
				<p class="mt-1 truncate text-sm text-muted">{saida.observacao ?? '—'}</p>
			</div>
		</div>

		<!-- Itens da saída -->
		<section class="overflow-hidden rounded-xl border border-border bg-surface">
			<h2 class="border-b border-border px-6 py-4 text-xs font-medium uppercase tracking-wide text-muted">
				Itens da saída
			</h2>
			<div class="overflow-x-auto">
				<table class="w-full text-sm">
					<thead>
						<tr class="border-b border-border text-left text-[11px] uppercase tracking-wide text-muted">
							<th class="px-6 py-2.5 font-medium">Item</th>
							<th class="px-6 py-2.5 text-right font-medium">Quantidade</th>
							<th class="px-6 py-2.5 font-medium">Destino</th>
						</tr>
					</thead>
					<tbody class="divide-y divide-border">
						<tr data-testid="exit-item-row">
							<td class="px-6 py-3">
								<p class="font-mono text-xs text-muted">{saida.idItem}</p>
								<p class="text-sm text-ink">{saida.nomeItem ?? 'Item do estoque'}</p>
							</td>
							<td class="px-6 py-3 text-right font-mono text-danger">
								−{fmtQty(saida.quantidade)}
							</td>
							<td class="px-6 py-3 text-muted">{destino || '—'}</td>
						</tr>
					</tbody>
				</table>
			</div>
		</section>

		<!-- Detalhes do registro -->
		<section class="rounded-xl border border-border bg-surface p-6">
			<h2 class="mb-3 text-xs font-medium uppercase tracking-wide text-muted">
				Detalhes do registro
			</h2>
			<dl
				class="grid grid-cols-1 divide-y divide-border/50 overflow-hidden rounded-lg border border-border sm:grid-cols-[220px_1fr]"
			>
				{@render specRow('Data da saída', fmtDate(saida.dataSaida))}
				{@render specRow('Motivo da saída', motivoMeta?.label ?? '—')}
				{@render specRow('Destino / Projeto', destino || '—')}
				{@render specRow('Observações', saida.observacao ?? '—')}
			</dl>
		</section>
	</div>
{/if}

{#snippet specRow(label: string, value: string)}
	<div class="contents">
		<dt class="bg-elevated/40 px-4 py-2.5 text-xs text-muted">{label}</dt>
		<dd class="px-4 py-2.5 text-sm text-ink">{value}</dd>
	</div>
{/snippet}