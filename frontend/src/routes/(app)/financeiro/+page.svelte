<script lang="ts">
	import { goto } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { CategoriaFinanceira } from '$lib/types/financeiro';
	import { lancamentoStatusMeta } from '$lib/utils/financeiro-status';
	import { formatSignedBRL } from '$lib/utils/financeiro-format';
	import { formatDateBR, formatMoneyBRL } from '$lib/utils/vendas-format';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import NovoLancamentoModal from './components/NovoLancamentoModal.svelte';

	let { data }: PageProps = $props();

	const resultado = $derived(data.resultado);
	const erro = $derived(data.error);
	const categorias = $derived(data.categorias as CategoriaFinanceira[]);
	const categoriasError = $derived(data.categoriasError);

	const lancamentos = $derived(resultado?.lancamentos ?? []);
	const resumo = $derived(resultado?.resumo);
	const counts = $derived(resultado?.counts);
	const total = $derived(resultado?.pagination.totalItems ?? 0);

	const carregando = $derived(resultado === null && erro === null);
	const comErro = $derived(erro !== null && resultado === null);

	function tentarNovamente(): void {
		void goto('/financeiro', { invalidateAll: true });
	}

	// Apenas escala de apresentação entre agregados servidos (sem somar itens).
	function largura(valor: number, maximo: number): number {
		if (maximo <= 0) return 0;
		return Math.max(4, Math.round((valor / maximo) * 100));
	}

	const maxFluxo = $derived(Math.max(resumo?.entradas ?? 0, resumo?.pendente ?? 0, 1));

	const atrasados = $derived(
		lancamentos.filter((l) => l.status === 'Atrasado').slice(0, 4)
	);
	const ultimos = $derived(lancamentos.slice(0, 5));
	const entradas = $derived(lancamentos.filter((l) => l.tipo === 'Entrada').slice(0, 3));
	const saidas = $derived(lancamentos.filter((l) => l.tipo === 'Saída').slice(0, 3));

	interface CategoriaContagem {
		nome: string;
		quantidade: number;
	}

	// Contagem de itens por categoria servida (sem somar valores).
	const categoriasTop = $derived.by((): CategoriaContagem[] => {
		const mapa = new Map<string, number>();
		for (const l of lancamentos) {
			mapa.set(l.categoria.nome, (mapa.get(l.categoria.nome) ?? 0) + 1);
		}
		return [...mapa.entries()]
			.map(([nome, quantidade]) => ({ nome, quantidade }))
			.sort((a, b) => b.quantidade - a.quantidade)
			.slice(0, 5);
	});
	const maxCategoria = $derived(Math.max(...categoriasTop.map((c) => c.quantidade), 1));

	// ---- Modal: novo lançamento (componente compartilhado) ----

	let modalNovo = $state(false);

	function abrirNovo(): void {
		modalNovo = true;
	}
</script>

<svelte:head>
	<title>Financeiro — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader title="Financeiro" subtitle="Visão geral do caixa do laboratório.">
		{#snippet children()}
			<button
				type="button"
				data-testid="fin-novo"
				onclick={abrirNovo}
				aria-label="Novo lançamento"
				class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
			>
				<Icon name="plus" class="h-4 w-4" /> Novo lançamento
			</button>
		{/snippet}
	</PageHeader>

	{#if comErro}
		<ErrorBanner
			message="Não foi possível carregar o resumo financeiro"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={tentarNovamente}
			testid="fin-retry"
		/>
	{:else if carregando}
		<div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4" aria-hidden="true">
			{#each Array(4) as _, i (i)}
				<div class="h-20 animate-pulse rounded-xl bg-elevated"></div>
			{/each}
		</div>
		<div class="grid gap-4 lg:grid-cols-3" aria-hidden="true">
			<div class="h-48 animate-pulse rounded-xl bg-elevated lg:col-span-2"></div>
			<div class="h-48 animate-pulse rounded-xl bg-elevated"></div>
		</div>
	{:else}
		{#if categoriasError}
			<p role="alert" class="rounded-xl border border-warn/30 bg-warn/10 px-4 py-2.5 text-xs text-warn">
				{categoriasError}. O modal de novo lançamento pode estar sem categorias.
			</p>
		{/if}

		{#if resumo && counts}
			<div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
				<div data-testid="fin-saldo" class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Entradas</p>
					<p class="mt-1 font-mono text-lg font-semibold text-success tabular-nums">
						{formatMoneyBRL(resumo.entradas)}
					</p>
					<p class="mt-0.5 text-xs text-muted">no período</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Saídas</p>
					<p class="mt-1 font-mono text-lg font-semibold text-danger tabular-nums">
						{formatMoneyBRL(resumo.saidas)}
					</p>
					<p class="mt-0.5 text-xs text-muted">no período</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">A receber</p>
					<p class="mt-1 font-mono text-lg font-semibold text-warn tabular-nums">
						{formatMoneyBRL(resumo.pendente)}
					</p>
					<p class="mt-0.5 text-xs text-danger">sendo {counts.Atrasado} atrasados</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Lançamentos</p>
					<p class="mt-1 font-mono text-lg font-semibold text-ink tabular-nums">{total}</p>
					<p class="mt-0.5 text-xs text-muted">{counts.Pago} pagos · {counts.Pendente} pendentes</p>
				</div>
			</div>
		{/if}

		<div class="grid gap-4 lg:grid-cols-3">
			<section aria-label="Fluxo de caixa" class="rounded-xl border border-border bg-surface p-5 lg:col-span-2">
				<div class="flex items-center justify-between gap-2">
					<h2 class="text-sm font-semibold text-ink">Fluxo de caixa</h2>
					<a
						href="/financeiro/relatorios"
						class="text-xs font-medium text-brandhi transition-colors hover:text-brand"
					>
						Ver relatórios
					</a>
				</div>
				{#if resumo}
					<div class="mt-4 space-y-3">
						<div>
							<div class="flex items-center justify-between text-xs">
								<span class="text-muted">Recebido</span>
								<span class="font-mono text-success tabular-nums">{formatMoneyBRL(resumo.entradas)}</span>
							</div>
							<div class="mt-1 h-2 rounded bg-elevated" role="img" aria-label="Recebido {formatMoneyBRL(resumo.entradas)}">
								<div
									class="h-2 rounded bg-success"
									style="width: {largura(resumo.entradas, maxFluxo)}%"
								></div>
							</div>
						</div>
						<div>
							<div class="flex items-center justify-between text-xs">
								<span class="text-muted">Previsto (pendente)</span>
								<span class="font-mono text-brandhi tabular-nums">{formatMoneyBRL(resumo.pendente)}</span>
							</div>
							<div class="mt-1 h-2 rounded bg-elevated" role="img" aria-label="Previsto {formatMoneyBRL(resumo.pendente)}">
								<div
									class="h-2 rounded bg-brand"
									style="width: {largura(resumo.pendente, maxFluxo)}%"
								></div>
							</div>
						</div>
					</div>
					<div class="mt-3 flex items-center gap-4 text-[10px] text-muted">
						<span class="inline-flex items-center gap-1.5">
							<span class="h-1.5 w-1.5 rounded-full bg-success" aria-hidden="true"></span> Recebido
						</span>
						<span class="inline-flex items-center gap-1.5">
							<span class="h-1.5 w-1.5 rounded-full bg-brand" aria-hidden="true"></span> Previsto
						</span>
					</div>
				{:else}
					<p class="mt-4 text-xs text-muted">Sem dados de fluxo no momento.</p>
				{/if}
			</section>

			<section aria-label="Alertas" class="rounded-xl border border-border bg-surface p-5">
				<h2 class="text-sm font-semibold text-ink">Alertas</h2>
				<div class="mt-3 space-y-2">
					{#if atrasados.length === 0}
						<div class="rounded-lg border border-success/30 bg-success/10 px-3 py-2.5">
							<p class="text-xs font-medium text-success">Nenhum lançamento atrasado.</p>
						</div>
					{:else}
						{#each atrasados as a (a.id)}
							<a
								href="/financeiro/lancamentos?status=Atrasado"
								data-testid="fin-alerta"
								data-goto="/financeiro/lancamentos?status=Atrasado"
								class="block rounded-lg border border-danger/30 bg-danger/10 px-3 py-2.5 transition hover:bg-danger/15"
							>
								<p class="text-xs font-medium text-danger">{a.codigo} atrasado</p>
								<p class="mt-0.5 font-mono text-xs text-muted">
									Venceu em {formatDateBR(a.dataVencimento)} · {formatSignedBRL(a.valor, a.tipo)}
								</p>
							</a>
						{/each}
					{/if}
					{#if counts && counts.Pendente > 0}
						<a
							href="/financeiro/lancamentos?status=Pendente"
							data-goto="/financeiro/lancamentos?status=Pendente"
							class="block rounded-lg border border-warn/30 bg-warn/10 px-3 py-2.5 transition hover:bg-warn/15"
						>
							<p class="text-xs font-medium text-warn">{counts.Pendente} lançamentos pendentes</p>
							<p class="mt-0.5 text-xs text-muted">Acompanhe os vencimentos em Lançamentos.</p>
						</a>
					{/if}
				</div>
			</section>
		</div>

		<div class="grid gap-4 lg:grid-cols-2">
			<section aria-label="Últimos lançamentos" class="rounded-xl border border-border bg-surface p-5">
				<div class="flex items-center justify-between gap-2">
					<h2 class="text-sm font-semibold text-ink">Últimos lançamentos</h2>
					<a
						href="/financeiro/lancamentos"
						class="text-xs font-medium text-brandhi transition-colors hover:text-brand"
					>
						Ver todos
					</a>
				</div>
				{#if ultimos.length === 0}
					<p class="mt-4 text-xs text-muted">Nenhum lançamento registrado.</p>
				{:else}
					<ul class="mt-3 space-y-2">
						{#each ultimos as l (l.id)}
							{@const st = lancamentoStatusMeta(l.status)}
							<li class="flex items-center justify-between gap-3 rounded-lg bg-elevated/50 px-3 py-2">
								<div class="min-w-0">
									<p class="truncate text-xs font-medium text-ink">{l.categoria.nome}</p>
									<p class="font-mono text-[10px] text-muted">{l.codigo}</p>
								</div>
								<div class="flex shrink-0 items-center gap-2">
									<span
										class="font-mono text-xs tabular-nums {l.tipo === 'Entrada' ? 'text-success' : 'text-danger'}"
									>
										{formatSignedBRL(l.valor, l.tipo)}
									</span>
									<StatusBadge label={st.label} color={st.color} />
								</div>
							</li>
						{/each}
					</ul>
				{/if}
			</section>

			<div class="space-y-4">
				<section aria-label="Entradas e saídas" class="rounded-xl border border-border bg-surface p-5">
					<h2 class="text-sm font-semibold text-ink">Entradas e saídas</h2>
					<div class="mt-3 grid gap-3 sm:grid-cols-2">
						<div>
							<p class="text-[10px] uppercase tracking-wide text-muted">Entradas</p>
							<ul class="mt-1.5 space-y-1.5">
								{#each entradas as l (l.id)}
									<li class="flex items-center justify-between gap-2 text-xs">
										<span class="truncate text-ink">{l.categoria.nome}</span>
										<span class="shrink-0 font-mono text-success tabular-nums">
											{formatSignedBRL(l.valor, l.tipo)}
										</span>
									</li>
								{:else}
									<li class="text-xs text-muted">Sem entradas.</li>
								{/each}
							</ul>
						</div>
						<div>
							<p class="text-[10px] uppercase tracking-wide text-muted">Saídas</p>
							<ul class="mt-1.5 space-y-1.5">
								{#each saidas as l (l.id)}
									<li class="flex items-center justify-between gap-2 text-xs">
										<span class="truncate text-ink">{l.categoria.nome}</span>
										<span class="shrink-0 font-mono text-danger tabular-nums">
											{formatSignedBRL(l.valor, l.tipo)}
										</span>
									</li>
								{:else}
									<li class="text-xs text-muted">Sem saídas.</li>
								{/each}
							</ul>
						</div>
					</div>
				</section>

			<section aria-label="Categorias" class="rounded-xl border border-border bg-surface p-5">
				<h2 class="text-sm font-semibold text-ink">Categorias mais frequentes</h2>
					{#if categoriasTop.length === 0}
						<p class="mt-3 text-xs text-muted">Nenhuma categoria com movimento.</p>
					{:else}
						<ul class="mt-3 space-y-2.5">
							{#each categoriasTop as c (c.nome)}
								<li>
									<div class="flex items-center justify-between text-xs">
										<span class="text-ink">{c.nome}</span>
										<span class="font-mono text-muted tabular-nums">{c.quantidade}</span>
									</div>
									<div
										class="mt-1 h-1.5 rounded bg-elevated"
										role="img"
										aria-label="{c.nome}: {c.quantidade} lançamentos"
									>
										<div
											class="h-1.5 rounded bg-brand"
											style="width: {largura(c.quantidade, maxCategoria)}%"
										></div>
									</div>
								</li>
							{/each}
						</ul>
					{/if}
				</section>
			</div>
		</div>
	{/if}
</div>

<NovoLancamentoModal
	open={modalNovo}
	categorias={categorias}
	idPrefix="fin-novo"
	onClose={() => (modalNovo = false)}
	onCreated={() => goto('/financeiro', { invalidateAll: true })}
/>
