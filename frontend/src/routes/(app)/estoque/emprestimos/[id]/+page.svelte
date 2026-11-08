<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { Tone } from '$lib/types/stock';
	import { fmtDate, fmtQty, daysFromToday } from '$lib/utils/stock-format';
	import { loanComputed, loanComputedMeta, loanStatusMeta } from '$lib/utils/stock-status';
	import Icon from '$lib/components/ui/Icon.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';

	let { data }: PageProps = $props();

	const canEdit = $derived(data.canEdit ?? false);
	const emprestimo = $derived(data.emprestimo);
	const loadError = $derived(data.error);
	const notFound = $derived(data.notFound);

	const tomador = $derived(data.pessoaNome || data.pessoaFallback);
	const devolvido = $derived(emprestimo?.status === 'DEVOLVIDO');

	function prazoMeta(): { label: string; color: Tone; chip?: string } {
		if (!emprestimo) return { label: '—', color: 'muted' };
		const meta = loanComputedMeta(loanComputed(emprestimo));
		if (devolvido) {
			return {
				label: fmtDate(emprestimo.dataDevolucaoReal),
				color: 'muted',
				chip: 'devolvido'
			};
		}
		const dias = Math.max(0, -daysFromToday(emprestimo.dataDevolucaoPrevista));
		if (dias > 0) {
			return {
				label: fmtDate(emprestimo.dataDevolucaoPrevista),
				color: 'danger',
				chip: `+${dias}d`
			};
		}
		if (meta.color === 'warn') {
			return { label: fmtDate(emprestimo.dataDevolucaoPrevista), color: 'warn', chip: 'hoje' };
		}
		return { label: fmtDate(emprestimo.dataDevolucaoPrevista), color: meta.color };
	}

	const chipClasses: Record<Tone, string> = {
		success: 'bg-muted/10 text-muted border border-border',
		warn: 'bg-warn/10 text-warn border border-warn/30',
		danger: 'bg-danger/10 text-danger border border-danger/30',
		brand: 'bg-brand/10 text-brandhi border border-brand/30',
		muted: 'bg-muted/10 text-muted border border-border',
		ink: 'bg-ink/10 text-ink border border-border'
	};
</script>

<svelte:head>
	<title>Empréstimo — Estoque — FabLab</title>
</svelte:head>

{#if loadError && !emprestimo}
	{#if notFound}
		<div data-testid="emprestimo-empty" class="rounded-xl border border-border bg-surface">
			<div class="flex flex-col items-center justify-center px-6 py-16 text-center">
				<span
					class="mb-4 flex h-12 w-12 items-center justify-center rounded-xl border border-brand/30 bg-brand/10"
				>
					<Icon name="arrow-uturn-left" class="h-5 w-5 text-brand" />
				</span>
				<h3 class="mb-1 text-sm font-semibold text-ink">Empréstimo não encontrado</h3>
				<p class="mb-5 max-w-xs text-xs text-muted">
					O empréstimo pode ter sido removido ou o endereço está incorreto. Verifique e tente
					novamente.
				</p>
				<a
					href="/estoque/emprestimos"
					class="inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3.5 py-2 text-xs font-medium text-ink transition-colors hover:bg-elevated/70"
				>
					<Icon name="chevron-left" class="h-3.5 w-3.5" /> Voltar para empréstimos
				</a>
			</div>
		</div>
	{:else}
		<div data-testid="emprestimo-error-retry">
			<ErrorBanner
				message="Não foi possível carregar o empréstimo"
				hint={loadError}
				onRetry={() => void invalidateAll()}
			/>
		</div>
	{/if}
{:else if !emprestimo}
	<div data-testid="emprestimo-loading" class="space-y-4">
		<div class="rounded-xl border border-border bg-surface p-6">
			<Skeleton class="h-5 w-40" />
			<Skeleton class="mt-3 h-8 w-72" />
			<Skeleton class="mt-2 h-3 w-64 opacity-60" />
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
		</div>
	</div>
{:else}
	{@const prazo = prazoMeta()}
	{@const status = loanStatusMeta(emprestimo.status)}
	<div data-testid="emprestimo-detalhe" class="space-y-4">
		<div class="flex items-center justify-between gap-4">
			<a
				href="/estoque/emprestimos"
				class="inline-flex items-center gap-2 text-xs font-medium text-muted transition-colors hover:text-ink"
			>
				<Icon name="chevron-left" class="h-3.5 w-3.5" /> Voltar para empréstimos
			</a>
			<StatusBadge label={status.label} color={status.color} />
		</div>

		<!-- Hero -->
		<section class="rounded-xl border border-border bg-surface p-6">
			<div class="flex flex-wrap items-center gap-5">
				<div
					class="flex h-24 w-24 shrink-0 items-center justify-center rounded-xl border border-border bg-elevated"
				>
					<Icon name="arrow-uturn-left" class="h-10 w-10 text-brand" />
				</div>
				<div class="min-w-0 flex-1">
					<p class="font-mono text-xs text-muted">EMPRESTIMO-{emprestimo.id}</p>
					<h1 class="mt-0.5 text-2xl font-semibold tracking-tight text-ink">Empréstimo de item</h1>
					<p class="mt-1 text-sm text-muted">
						<span class="font-mono text-brand">{emprestimo.idItem}</span>
						{emprestimo.nomeItem ? ` · ${emprestimo.nomeItem}` : ''}
						<span class="mx-1.5 text-muted/50">·</span> Tomado em {fmtDate(emprestimo.dataEmprestimo)} por
						<strong class="font-medium text-ink">{tomador}</strong>
					</p>
					<div class="mt-3 flex items-baseline gap-2">
						<span class="text-2xl font-semibold tracking-tight text-brandhi">
							{fmtQty(emprestimo.quantidade)}
						</span>
						<span class="text-xs text-muted">emprestados</span>
						<span
							class="ml-1 rounded px-1.5 py-0.5 text-[10px] font-medium {chipClasses[prazo.color]}"
						>
							{prazo.chip ?? (prazo.color === 'success' ? 'no prazo' : '')}
						</span>
					</div>
				</div>
			</div>
		</section>

		<!-- Timeline -->
		<section class="rounded-xl border border-border bg-surface p-6">
			<h2 class="mb-4 text-xs font-medium uppercase tracking-wide text-muted">Linha do tempo</h2>
			<ol class="relative space-y-4 border-l-2 border-border pl-6">
				<li class="relative">
					<span
						class="absolute -left-[31px] flex h-4 w-4 items-center justify-center rounded-full bg-brand/20"
					>
						<span class="h-2 w-2 rounded-full bg-brand"></span>
					</span>
					<p class="text-sm font-medium text-ink">Empréstimo realizado</p>
					<p class="font-mono text-xs text-muted">{fmtDate(emprestimo.dataEmprestimo)}</p>
				</li>
				<li class="relative">
					<span
						class="absolute -left-[31px] flex h-4 w-4 items-center justify-center rounded-full {devolvido
							? 'bg-success/20'
							: prazo.color === 'danger'
								? 'bg-danger/20'
								: prazo.color === 'warn'
									? 'bg-warn/20'
									: 'bg-muted/20'}"
					>
						<span
							class="h-2 w-2 rounded-full {devolvido
								? 'bg-success'
								: prazo.color === 'danger'
									? 'bg-danger'
									: prazo.color === 'warn'
										? 'bg-warn'
										: 'bg-muted'}"
						></span>
					</span>
					<p class="text-sm font-medium text-ink">Previsão de devolução</p>
					<p class="font-mono text-xs text-muted">
						{fmtDate(emprestimo.dataDevolucaoPrevista)}
						{#if !devolvido && prazo.chip}
							<span class="ml-1 rounded px-1.5 py-0.5 text-[10px] font-medium {chipClasses[prazo.color]}">
								{prazo.chip}
							</span>
						{/if}
					</p>
				</li>
				<li class="relative">
					<span
						class="absolute -left-[31px] flex h-4 w-4 items-center justify-center rounded-full {devolvido
							? 'bg-success/20'
							: 'bg-muted/10'}"
					>
						<span
							class="h-2 w-2 rounded-full {devolvido ? 'bg-success' : 'bg-muted'}"
						></span>
					</span>
					<p class="text-sm font-medium text-ink">Devolução</p>
					<p class="font-mono text-xs text-muted">
						{emprestimo.dataDevolucaoReal ? fmtDate(emprestimo.dataDevolucaoReal) : 'Pendente'}
					</p>
				</li>
			</ol>
		</section>

		<!-- Dados do empréstimo -->
		<section class="rounded-xl border border-border bg-surface p-6">
			<h2 class="mb-3 text-xs font-medium uppercase tracking-wide text-muted">
				Dados do empréstimo
			</h2>
			<dl
				class="grid grid-cols-1 divide-y divide-border/50 overflow-hidden rounded-lg border border-border sm:grid-cols-2"
			>
				{@render specRow('Item', `${emprestimo.idItem}${emprestimo.nomeItem ? ` — ${emprestimo.nomeItem}` : ''}`, true)}
				{@render specRow('Tomador', tomador)}
				{@render specRow('Quantidade', fmtQty(emprestimo.quantidade))}
				{@render specRow('Data do empréstimo', fmtDate(emprestimo.dataEmprestimo))}
				{@render specRow('Previsão de devolução', fmtDate(emprestimo.dataDevolucaoPrevista))}
				{@render specRow('Devolução real', emprestimo.dataDevolucaoReal ? fmtDate(emprestimo.dataDevolucaoReal) : 'Pendente')}
			</dl>
			{#if emprestimo.observacao}
				<div class="mt-4">
					<p class="mb-1 text-xs font-medium text-muted">Observações</p>
					<p class="rounded-lg border border-border bg-elevated/50 px-3 py-2.5 text-sm text-ink">
						{emprestimo.observacao}
					</p>
				</div>
			{/if}
		</section>

		<!-- Ações -->
		{#if canEdit && !devolvido}
			<div class="sticky bottom-4 z-10 flex justify-end">
				<a
					data-testid="btn-devolucao"
					href={`/estoque/emprestimos/${emprestimo.id}/devolucao`}
					class="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white shadow-lg shadow-brand/25 transition-colors hover:bg-brandhi"
				>
					<Icon name="arrow-uturn-left" class="h-4 w-4" /> Registrar devolução
				</a>
			</div>
		{/if}
	</div>
{/if}

{#snippet specRow(label: string, value: string, link?: boolean)}
	<div class="contents">
		<dt class="bg-elevated/40 px-4 py-2.5 text-xs text-muted">{label}</dt>
		<dd class="px-4 py-2.5 text-sm text-ink">
			{#if link && emprestimo?.idItem}
				<a href={`/estoque/itens/${emprestimo.idItem}`} class="font-mono text-brand hover:underline">
					{value}
				</a>
			{:else}
				{value}
			{/if}
		</dd>
	</div>
{/snippet}