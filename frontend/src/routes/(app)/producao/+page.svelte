<script lang="ts">
	import { goto } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { AlertaProducao, KpisProducao } from '$lib/types/producao';
	import { CARTAO_META, INSPECAO_STATUS_META, KANBAN_COLUNA_META, MAQUINA_STATUS_META } from '$lib/utils/producao-status';
	import CartaoBadge from '$lib/components/producao/CartaoBadge.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import { USE_PRODUCAO_MOCK } from '$lib/api/producao/mocks';

	let { data }: PageProps = $props();

	const resumo = $derived(data.resumo);
	const error = $derived(data.error);

	const kpis = $derived(resumo?.kpis as KpisProducao | undefined);
	const loading = $derived(resumo === null && error === null);

	const hasError = $derived(error !== null && resumo === null);

	function formatarData(iso: string): string {
		if (!iso) return '—';
		const [a, m, d] = iso.slice(0, 10).split('-');
		return d ? `${d}/${m}/${a}` : '—';
	}

	const ALERTA_TONES: Record<AlertaProducao['severidade'], string> = {
		danger: 'border-danger/30 bg-danger/10 text-danger',
		warn: 'border-warn/30 bg-warn/10 text-warn',
		success: 'border-success/30 bg-success/10 text-success',
		info: 'border-brand/30 bg-brand/10 text-brandhi'
	};

	const ALERTA_DOT: Record<AlertaProducao['severidade'], string> = {
		danger: 'bg-danger',
		warn: 'bg-warn',
		success: 'bg-success',
		info: 'bg-brand'
	};

	const colunas = ['Fila', 'Produção', 'Acabamento', 'Pronto', 'Entregue'] as const;
	const valoresKanban = $derived(colunas.map((c) => kpis?.kanban[c] ?? 0));
	const maxKanban = $derived(Math.max(1, ...valoresKanban));

	function ir(gotoPath: string | undefined): void {
		if (gotoPath) void goto(gotoPath);
	}

	const temSetores = $derived((resumo?.setores.length ?? 0) > 0);
	const temRanking = $derived((resumo?.rankingTop.length ?? 0) > 0);
	const temMaquinas = $derived((resumo?.maquinas.length ?? 0) > 0);
	const temAlertas = $derived((resumo?.alertas.length ?? 0) > 0);
</script>

<svelte:head>
	<title>Produção — Resumo — FabLab</title>
</svelte:head>

{#if USE_PRODUCAO_MOCK}
	<div
		data-testid="prd-mock-banner"
		class="mb-5 flex items-start gap-3 rounded-xl border border-warn/30 bg-warn/10 px-4 py-3 text-xs text-muted"
	>
		<span class="mt-0.5 text-warn" aria-hidden="true">🟡</span>
		<div class="flex-1">
			<b class="text-ink">Dados de exemplo — contrato 🟡 pendente.</b>{' '}
			Os dados voltam a ser reais quando o gateway expor{' '}
			<span class="font-mono">/api/producao/**</span>.
		</div>
	</div>
{/if}

{#if hasError}
	<ErrorBanner
		message="Não foi possível carregar o resumo da Produção"
		hint={error ?? ''}
		onRetry={() => void goto('/producao', { invalidateAll: true })}
	/>
{:else if loading}
	<!-- LOADING -->
	<div class="grid grid-cols-1 gap-4 lg:grid-cols-4">
		{#each [0, 1, 2, 3] as i (i)}
			<div class="space-y-2 rounded-xl border border-border bg-surface p-4">
				<Skeleton class="h-3 w-24" />
				<Skeleton class="h-7 w-16" />
			</div>
		{/each}
	</div>
	<section class="rounded-xl border border-border bg-surface p-5">
		<Skeleton class="h-4 w-40" />
		<div class="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
			{#each [0, 1, 2, 3] as i (i)}
				<div class="space-y-2 rounded-xl border border-border bg-elevated/40 p-4">
					<Skeleton class="h-3 w-20" />
					<Skeleton class="h-4 w-3/4" />
					<Skeleton class="h-3 w-1/2" />
				</div>
			{/each}
		</div>
	</section>
	<div class="grid grid-cols-1 gap-4 lg:grid-cols-3">
		<section class="rounded-xl border border-border bg-surface p-5">
			<Skeleton class="h-4 w-32" />
			<div class="mt-4 space-y-2">
				{#each [0, 1, 2] as i (i)}
					<Skeleton class="h-12 w-full" />
				{/each}
			</div>
		</section>
		<section class="rounded-xl border border-border bg-surface p-5">
			<Skeleton class="h-4 w-24" />
			<div class="mt-4 space-y-2">
				{#each [0, 1] as i (i)}
					<Skeleton class="h-16 w-full" />
				{/each}
			</div>
		</section>
	</div>
{:else}
	<!-- CONTEÚDO -->
	<div data-testid="prd-kpi" class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
		<div class="rounded-xl border border-border bg-surface p-4">
			<span class="text-xs text-muted">Projetos ativos</span>
			<p class="mt-1 text-2xl font-semibold tracking-tight text-ink tabular-nums">{kpis?.projetosAtivos ?? 0}</p>
		</div>
		<div class="rounded-xl border border-border bg-surface p-4">
			<span class="text-xs text-muted">Tarefas pendentes</span>
			<p class="mt-1 text-2xl font-semibold tracking-tight text-ink tabular-nums">{kpis?.tarefasPendentes ?? 0}</p>
			<p class="mt-1 text-xs text-danger">{kpis?.tarefasAtrasadas ?? 0} atrasadas</p>
		</div>
		<div class="rounded-xl border border-border bg-surface p-4">
			<span class="text-xs text-muted">Máquinas</span>
			<p class="mt-1 text-2xl font-semibold tracking-tight text-ink tabular-nums">{kpis?.maquinas ?? 0}</p>
			<p class="mt-1 text-xs text-warn">{kpis?.maquinasForaVerde ?? 0} fora da verde</p>
		</div>
		<div class="rounded-xl border border-border bg-surface p-4">
			<span class="text-xs text-muted">Sistema 5S</span>
			<p class="mt-1 text-2xl font-semibold tracking-tight text-ink tabular-nums">{kpis?.cartoes.verde ?? 0} verdes</p>
			<p class="mt-1 text-xs text-muted">
				{kpis?.cartoes.amarelo ?? 0} amarelos · {kpis?.cartoes.vermelho ?? 0} vermelhos
			</p>
		</div>
	</div>

	<!-- ALERTAS -->
	<section aria-label="Alertas de produção" class="space-y-2">
		<h2 class="text-sm font-semibold text-ink">Alertas</h2>
		{#if !temAlertas}
			<div class="rounded-xl border border-border bg-surface">
				<EmptyState icon="info" title="Nenhum alerta" description="Tudo sob controle na produção." />
			</div>
		{:else}
			{#each resumo?.alertas ?? [] as alerta (alerta.id)}
				<button
					type="button"
					data-testid="prd-alerta"
					data-goto={alerta.dataGoto ?? ''}
					onclick={() => ir(alerta.dataGoto)}
					class="flex w-full items-start gap-3 rounded-xl border border-border bg-surface p-4 text-left transition hover:border-brand/40 hover:bg-elevated/30"
				>
					<span
						class="mt-1 h-2 w-2 shrink-0 rounded-full {ALERTA_DOT[alerta.severidade]}"
						aria-hidden="true"
					></span>
					<span class="min-w-0 flex-1">
						<span class={`block text-sm font-medium ${ALERTA_TONES[alerta.severidade]}`}>{alerta.titulo}</span>
						{#if alerta.descricao}
							<span class="mt-0.5 block text-xs text-muted">{alerta.descricao}</span>
						{/if}
					</span>
					<span class="shrink-0 text-muted" aria-hidden="true">
						<Icon name="chevron-right" class="h-4 w-4" />
					</span>
				</button>
			{/each}
		{/if}
	</section>

	<!-- SETORES 5S -->
	<section aria-label="Setores 5S">
		<div class="flex items-center justify-between gap-2">
			<h2 class="text-sm font-semibold text-ink">Setores 5S</h2>
			<a href="/producao/5s/setores" class="text-xs font-medium text-brandhi transition hover:text-brand">
				Ver setores
			</a>
		</div>
		{#if !temSetores}
			<div class="mt-2 rounded-xl border border-border bg-surface">
				<EmptyState icon="squares" title="Nenhum setor 5S" description="Cadastre setores para iniciar o 5S." />
			</div>
		{:else}
			<div class="mt-2 grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
				{#each resumo?.setores ?? [] as setor (setor.id)}
					<a
						href="/producao/5s/setores"
						data-goto="setor-5s"
						class="rounded-xl border border-border bg-surface p-4 transition hover:border-brand/40"
					>
						<div class="flex items-center justify-between gap-2">
							<p class="text-sm font-semibold text-ink">{setor.nome}</p>
							<span class="rounded-full border border-border bg-elevated/60 px-2 py-0.5 text-[10px] font-medium capitalize text-muted">
								{setor.ciclo}
							</span>
						</div>
						<div class="mt-3 flex items-center justify-between gap-2">
							<CartaoBadge cartao={setor.cartao} />
							{#if setor.nota !== null && setor.nota !== undefined}
								<span class="font-mono text-sm text-ink tabular-nums">{setor.nota}</span>
							{/if}
						</div>
						<div class="mt-3 flex items-center gap-1.5">
							{#each setor.responsaveis.slice(0, 3) as resp (resp.membro.id)}
								<Avatar name={resp.membro.nome} size="xs" tone={resp.ps ? 'brand' : 'muted'} />
							{/each}
							<span class="ml-auto text-[11px] text-muted">audit. {formatarData(setor.proximaAuditoria)}</span>
						</div>
					</a>
				{/each}
			</div>
		{/if}
	</section>

	<div class="grid grid-cols-1 gap-4 lg:grid-cols-3">
		<!-- KANBAN -->
		<section data-testid="prd-kanban" aria-label="Encomendas em produção" class="lg:col-span-2 rounded-xl border border-border bg-surface p-5">
			<h2 class="text-sm font-semibold text-ink">Encomendas em produção</h2>
			<p class="text-[11px] text-muted">Kanban de encomendas é gerido em Vendas.</p>
			<div class="mt-4 grid grid-cols-5 gap-2">
				{#each colunas as coluna, idx (coluna)}
					{@const meta = KANBAN_COLUNA_META[coluna]}
					{@const total = valoresKanban[idx]}
					<div class="rounded-lg border border-border bg-elevated/30 p-2 text-center">
						<p class="text-[11px] font-medium text-muted">{meta.label}</p>
						<div class="mt-2 h-16 overflow-hidden rounded-md bg-elevated/60">
							<div
								class="w-full {total > 0 ? 'bg-brand/70' : ''}"
								style:height={`${Math.max(8, Math.round((total / maxKanban) * 100))}%`}
							></div>
						</div>
						<p class="mt-1.5 font-mono text-sm text-ink tabular-nums">{total}</p>
					</div>
				{/each}
			</div>
		</section>

		<!-- RANKING TOP -->
		<section aria-label="Ranking 5S do mês" class="rounded-xl border border-border bg-surface p-5">
			<div class="flex items-center justify-between gap-2">
				<h2 class="text-sm font-semibold text-ink">Ranking 5S do mês</h2>
				<a href="/producao/5s/ranking" class="text-xs font-medium text-brandhi transition hover:text-brand">
					ver ranking
				</a>
			</div>
			{#if !temRanking}
				<EmptyState icon="chart" title="Sem ranking ainda" description="Conclua auditorias para gerar o ranking." />
			{:else}
				<ol class="mt-3 space-y-2">
					{#each resumo?.rankingTop ?? [] as item (item.posicao)}
						<li
							class="flex items-center gap-3 rounded-lg border border-border bg-elevated/30 p-3 {item.posicao === 1
								? 'border-warn/40 bg-warn/5'
								: ''}"
						>
							<span class="w-5 font-mono text-sm font-semibold text-muted tabular-nums">{item.posicao}º</span>
							<Avatar name={item.responsavel.nome} size="xs" />
							<span class="min-w-0 flex-1 truncate text-sm text-ink">{item.responsavel.nome}</span>
							<span class="font-mono text-sm text-ink tabular-nums">{item.nota}</span>
							<CartaoBadge cartao={item.cartao} />
						</li>
					{/each}
				</ol>
			{/if}
		</section>
	</div>

	<!-- MÁQUINAS -->
	<section aria-label="Máquinas" class="rounded-xl border border-border bg-surface p-5">
		<h2 class="text-sm font-semibold text-ink">Máquinas</h2>
		{#if !temMaquinas}
			<EmptyState icon="wrench" title="Nenhuma máquina cadastrada" description="Cadastre máquinas para acompanhar aqui." />
		{:else}
			<div class="mt-3 grid grid-cols-1 gap-3 md:grid-cols-3">
				{#each resumo?.maquinas ?? [] as maquina (maquina.id)}
					<a
						href={`/producao/maquinas/${maquina.id}`}
						data-goto="maquina-detalhe"
						class="flex items-center gap-3 rounded-xl border border-border bg-elevated/30 p-3 transition hover:border-brand/40"
					>
						<span class="shrink-0 text-muted" aria-hidden="true">
							<Icon name="wrench" class="h-5 w-5" />
						</span>
						<span class="min-w-0 flex-1">
							<span class="block font-mono text-xs text-muted">{maquina.codigo}</span>
							<span class="block truncate text-sm font-medium text-ink">{maquina.nome}</span>
						</span>
						<span class="flex shrink-0 items-center gap-1.5">
							<CartaoBadge cartao={maquina.cartao} />
							<StatusBadge {...MAQUINA_STATUS_META[maquina.status]} />
						</span>
					</a>
				{/each}
			</div>
		{/if}
	</section>
{/if}