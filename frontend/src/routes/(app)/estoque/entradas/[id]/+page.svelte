<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import { fmtDate, fmtMoney, fmtQty } from '$lib/utils/stock-format';
	import { entryKindMeta } from '$lib/utils/stock-status';
	import Icon from '$lib/components/ui/Icon.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';

	let { data }: PageProps = $props();

	const entrada = $derived(data.entrada);
	const loadError = $derived(data.error);
	const notFound = $derived(data.notFound);

	const tipo = $derived(entrada?.idFornecedor ? ('compra' as const) : ('doacao' as const));
	const tipoMeta = $derived(entrada ? entryKindMeta(tipo) : null);
	const valorTotal = $derived(
		entrada
			? (entrada.valorTotal ??
				(entrada.valorUnitario != null ? entrada.valorUnitario * entrada.quantidade : null))
			: null
	);
	const fornecedor = $derived(
		entrada
			? (entrada.nomeFornecedor ?? entrada.fornecedor?.nome ?? 'Sem fornecedor vinculado')
			: ''
	);
</script>

<svelte:head>
	<title>Entrada — Estoque — FabLab</title>
</svelte:head>

{#if loadError && !entrada}
	{#if notFound}
		<div data-testid="entrada-empty" class="rounded-xl border border-border bg-surface">
			<div class="flex flex-col items-center justify-center px-6 py-16 text-center">
				<span
					class="mb-4 flex h-12 w-12 items-center justify-center rounded-xl border border-brand/30 bg-brand/10"
				>
					<Icon name="arrow-down-tray" class="h-5 w-5 text-brand" />
				</span>
				<h3 class="mb-1 text-sm font-semibold text-ink">Entrada não encontrada</h3>
				<p class="mb-5 max-w-xs text-xs text-muted">
					A entrada pode ter sido removida ou o endereço está incorreto. Verifique e tente novamente.
				</p>
				<a
					href="/estoque/entradas"
					class="inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3.5 py-2 text-xs font-medium text-ink transition-colors hover:bg-elevated/70"
				>
					<Icon name="chevron-left" class="h-3.5 w-3.5" /> Voltar para entradas
				</a>
			</div>
		</div>
	{:else}
		<div data-testid="entrada-error-retry">
			<ErrorBanner
				message="Não foi possível carregar a entrada"
				hint={loadError}
				onRetry={() => void invalidateAll()}
			/>
		</div>
	{/if}
{:else if !entrada}
	<div data-testid="entrada-loading" class="space-y-4">
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
	<div data-testid="entrada-detalhe" class="space-y-4">
		<div class="flex items-center justify-between gap-4">
			<a
				data-testid="back-lista"
				href="/estoque/entradas"
				class="inline-flex items-center gap-2 text-xs font-medium text-muted transition-colors hover:text-ink"
			>
				<Icon name="chevron-left" class="h-3.5 w-3.5" /> Voltar para entradas
			</a>
			{#if tipoMeta}
				<StatusBadge label={tipoMeta.label} color={tipoMeta.color} />
			{/if}
		</div>

		<!-- Hero -->
		<section class="rounded-xl border border-border bg-surface p-6">
			<div class="flex flex-wrap items-center gap-5">
				<div
					class="flex h-24 w-24 shrink-0 items-center justify-center rounded-xl border border-border bg-elevated"
				>
					<Icon name="arrow-down-tray" class="h-10 w-10 text-success" />
				</div>
				<div class="min-w-0 flex-1">
					<p class="font-mono text-xs text-muted">ENTRADA-{entrada.id}</p>
					<h1 class="mt-0.5 text-2xl font-semibold tracking-tight text-ink">Entrada de estoque</h1>
					<p class="mt-1 text-sm text-muted">
						<span class="font-mono text-brand">{entrada.idItem}</span>
						{entrada.nomeItem ? ` · ${entrada.nomeItem}` : ''}
						<span class="mx-1.5 text-muted/50">·</span> {fmtDate(entrada.dataEntrada)}
					</p>
					<div class="mt-3 flex items-baseline gap-2">
						<span class="text-2xl font-semibold tracking-tight text-success">
							+{fmtQty(entrada.quantidade)}
						</span>
						{#if entrada.notaFiscal}
							<span class="font-mono text-xs text-muted">NF {entrada.notaFiscal}</span>
						{/if}
					</div>
				</div>
			</div>
		</section>

		<!-- Metadata -->
		<div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
			<div class="rounded-xl border border-border bg-surface p-4">
				<span class="text-xs text-muted">Data</span>
				<p class="mt-1 font-mono text-sm font-medium text-ink">{fmtDate(entrada.dataEntrada)}</p>
			</div>
			<div class="rounded-xl border border-border bg-surface p-4">
				<span class="text-xs text-muted">Tipo</span>
				<p class="mt-1 text-sm font-medium text-ink">{tipoMeta?.label ?? '—'}</p>
			</div>
			<div class="rounded-xl border border-border bg-surface p-4">
				<span class="text-xs text-muted">Valor total</span>
				<p class="mt-1 font-mono text-sm font-medium text-ink">{fmtMoney(valorTotal)}</p>
			</div>
			<div class="rounded-xl border border-border bg-surface p-4">
				<span class="text-xs text-muted">Fornecedor</span>
				<p class="mt-1 truncate text-sm font-medium text-ink">{fornecedor}</p>
			</div>
		</div>

		<!-- Itens da entrada -->
		<section class="overflow-hidden rounded-xl border border-border bg-surface">
			<h2 class="border-b border-border px-6 py-4 text-xs font-medium uppercase tracking-wide text-muted">
				Itens da entrada
			</h2>
			<div class="overflow-x-auto">
				<table class="w-full text-sm">
					<thead>
						<tr class="border-b border-border text-left text-[11px] uppercase tracking-wide text-muted">
							<th class="px-6 py-2.5 font-medium">Item</th>
							<th class="px-6 py-2.5 text-right font-medium">Quantidade</th>
							<th class="px-6 py-2.5 text-right font-medium">Valor unitário</th>
							<th class="px-6 py-2.5 font-medium">Fornecedor</th>
						</tr>
					</thead>
					<tbody class="divide-y divide-border">
						<tr data-testid="entry-item-row">
							<td class="px-6 py-3">
								<p class="font-mono text-xs text-muted">{entrada.idItem}</p>
								<p class="text-sm text-ink">{entrada.nomeItem ?? 'Item do estoque'}</p>
							</td>
							<td class="px-6 py-3 text-right font-mono text-success">
								+{fmtQty(entrada.quantidade)}
							</td>
							<td class="px-6 py-3 text-right font-mono text-ink">
								{fmtMoney(entrada.valorUnitario)}
							</td>
							<td class="px-6 py-3 text-muted">{fornecedor}</td>
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
				{@render specRow('Data', fmtDate(entrada.dataEntrada))}
				{@render specRow('Tipo', tipoMeta?.label ?? '—')}
				{@render specRow('Valor total', fmtMoney(valorTotal))}
				{@render specRow('Fornecedor', fornecedor)}
				{@render specRow('Documento', entrada.notaFiscal ? `NF ${entrada.notaFiscal}` : '—')}
				{@render specRow('Observações', entrada.observacao ?? '—')}
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