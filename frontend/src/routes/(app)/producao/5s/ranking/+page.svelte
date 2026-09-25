<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import CartaoBadge from '$lib/components/producao/CartaoBadge.svelte';
	import { CARTAO_META } from '$lib/utils/producao-status';

	const MESES_ABREV = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];

	const kpiCls = 'bg-elevated border border-border rounded-xl p-4';
	const cellCls = 'px-5 py-3 text-sm whitespace-nowrap';
	const thCls = 'px-5 py-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted';

	let { data }: PageProps = $props();

	const loadingRanking = $derived(!data.result && !data.erroRanking);
	const loadingEvolucao = $derived(data.evolucao === null && !data.erroEvolucao);
	const ranking = $derived(data.result?.ranking ?? []);
	const mediaGeral = $derived(data.result?.mediaGeral ?? 0);

	const cartoes = $derived({
		verde: ranking.filter((r) => r.cartao === 'Verde').length,
		amarelo: ranking.filter((r) => r.cartao === 'Amarelo').length,
		vermelho: ranking.filter((r) => r.cartao === 'Vermelho').length
	});

	const top1 = $derived(ranking.find((r) => r.top1) ?? null);

	const evolucao = $derived(
		(data.evolucao ?? []).map((e) => {
			const notas = e.pontos.map((p) => p.nota);
			const soma = notas.reduce((acc, n) => acc + n, 0);
			const media = notas.length ? soma / notas.length : 0;
			const primeiro = notas[0] ?? 0;
			const ultimo = notas[notas.length - 1] ?? 0;
			const tendencia = ultimo > primeiro ? 'caindo' : ultimo < primeiro ? 'subindo' : 'estavel';
			return { ...e, media, tendencia };
		})
	);

	const todosMeses = $derived(
		[...new Set((data.evolucao ?? []).flatMap((e) => e.pontos.map((p) => p.mes)))].sort()
	);

	function trocarMes(event: Event) {
		const proximo = (event.currentTarget as HTMLInputElement).value;
		if (proximo && proximo !== data.params.mes) goto(`?mes=${proximo}`);
	}

	function abrevMes(mes: string) {
		const idx = Number(mes.slice(5, 7)) - 1;
		return Number.isInteger(idx) && idx >= 0 && idx <= 11 ? MESES_ABREV[idx] : mes;
	}

	function formatarNota(nota: number) {
		return nota.toLocaleString('pt-BR', { maximumFractionDigits: 1 });
	}
</script>

<svelte:head><title>Ranking 5S · Produção</title></svelte:head>

<section class="space-y-5">
	<PageHeader
		title="Ranking 5S"
		subtitle="Posição dos responsáveis por setor e evolução das notas de auditoria."
	>
		<label class="flex items-center gap-2 text-sm">
			<span class="text-muted">Mês</span>
			<input
				aria-label="Mês do ranking"
				type="month"
				value={data.params.mes}
				onchange={trocarMes}
				class="bg-elevated border border-border rounded-md px-3 py-2 text-sm focus:outline-none focus:border-brand focus:ring-2 focus:ring-brand/30"
			/>
		</label>
	</PageHeader>

	<div class="rounded-lg bg-elevated/40 border border-border p-3 text-xs text-muted">
		<b class="text-ink">Regra</b>
		Top 1 do mês fica <b class="text-ink">livre de auditoria por 1 semana</b>.
		Nota <b class="text-ink">≥ 90</b> garante cartão verde; <b class="text-ink">&lt; 70</b>, cartão vermelho.
	</div>

	{#if loadingRanking}
		<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
			{#each Array(4) as _, i}
				<div
					class="h-24 bg-elevated border border-border rounded-xl animate-pulse"
					role="status"
					aria-label="Carregando indicadores {i + 1} de 4"
				></div>
			{/each}
		</div>
		<div class="h-52 bg-elevated border border-border rounded-xl animate-pulse" role="status">
			<span class="sr-only">Carregando ranking...</span>
		</div>
	{:else if data.erroRanking}
		<ErrorBanner
			message={data.erroRanking}
			onRetry={() => invalidateAll()}
		/>
	{:else}
		<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4" data-testid="rank-kpis">
			<div class={kpiCls}>
				<span class="text-xs text-muted">Média geral</span>
				<div class="mt-1 text-2xl font-semibold tabnums">{formatarNota(mediaGeral)}</div>
			</div>
			<div class={kpiCls}>
				<span class="text-xs text-muted">Participantes</span>
				<div class="mt-1 text-2xl font-semibold tabnums">{ranking.length}</div>
			</div>
			<div class={kpiCls}>
				<span class="text-xs text-muted">Cartões verdes</span>
				<div class="mt-1 text-2xl font-semibold tabnums text-success">{cartoes.verde}</div>
			</div>
			<div class={kpiCls}>
				<span class="text-xs text-muted">Vermelhos</span>
				<div class="mt-1 text-2xl font-semibold tabnums {cartoes.vermelho ? 'text-danger' : ''}">
					{cartoes.vermelho}
				</div>
			</div>
		</div>

		{#if ranking.length === 0}
			<EmptyState
				icon="chart"
				title="Sem notas neste mês"
				description="Quando houver auditorias concluídas no período, o ranking será montado aqui."
			/>
		{:else}
			<div class="bg-elevated border border-border rounded-xl overflow-x-auto">
				<table class="w-full text-sm">
					<thead>
						<tr class="border-b border-border">
							<th class={thCls}>Posição</th>
							<th class={thCls}>Responsável</th>
							<th class={thCls}>Nota</th>
							<th class={thCls}>Cartão</th>
							<th class={thCls}>Situação</th>
						</tr>
					</thead>
					<tbody class="divide-y divide-border">
						{#each ranking as item (item.responsavel.id)}
							{@const eTop1 = item.top1}
							<tr data-testid="rank-row" class={eTop1 ? 'bg-brand/5' : ''}>
								<td class={cellCls}>
									<span
										class={`inline-flex h-7 w-7 items-center justify-center rounded-full text-xs font-semibold ${
											eTop1
												? 'bg-warn/20 text-warn'
												: item.posicao === 2
													? 'bg-muted/15 text-muted'
													: item.posicao === 3
														? 'bg-danger/10 text-danger'
														: 'bg-elevated border border-border text-muted'
										}`}
									>
										{item.posicao}
									</span>
								</td>
								<td class={cellCls}>
									<div class="flex items-center gap-2">
										<Avatar name={item.responsavel.nome} size="xs" tone="brand" />
										<span class="font-medium">{item.responsavel.nome}</span>
									</div>
								</td>
								<td class={`${cellCls} tabnums font-semibold`}>{formatarNota(item.nota)}</td>
								<td class={cellCls}>
									<CartaoBadge cartao={item.cartao} label={CARTAO_META[item.cartao].label} />
								</td>
								<td class={cellCls}>
									{#if eTop1}
										<span
											data-testid="rank-top1"
											class="inline-flex items-center gap-1 rounded-full border border-warn/40 bg-warn/15 px-2.5 py-0.5 text-xs font-semibold text-warn"
										>
											<span class="h-1.5 w-1.5 rounded-full bg-warn" aria-hidden="true"></span>
											Top 1 livre
										</span>
									{:else if item.semanaLivre}
										<span class="text-xs text-muted">semana livre</span>
									{:else}
										<span class="text-muted text-xs">—</span>
									{/if}
								</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>
			{#if top1}
				<div class="rounded-lg bg-success/5 border border-success/30 p-3 text-xs text-muted">
					<b class="text-success">{top1.responsavel.nome}</b> é o Top 1 do mês e está
					<b class="text-ink">livre de auditoria por 1 semana</b>.
				</div>
			{/if}
		{/if}
	{/if}

	<div>
		<h2 class="text-sm font-semibold tracking-tight mb-3">Evolução das notas</h2>
		{#if loadingEvolucao}
			<div class="h-40 bg-elevated border border-border rounded-xl animate-pulse" role="status">
				<span class="sr-only">Carregando evolução...</span>
			</div>
		{:else if data.erroEvolucao}
			<ErrorBanner
				message={data.erroEvolucao}
				onRetry={() => invalidateAll()}
			/>
		{:else if evolucao.length === 0}
			<div class="rounded-lg bg-elevated/40 border border-border p-6 text-sm text-muted">
				Nenhuma evolução para {abrevMes(data.params.mes)}.
			</div>
		{:else}
			<div class="bg-elevated border border-border rounded-xl overflow-x-auto">
				<table class="w-full text-sm">
					<thead>
						<tr class="border-b border-border">
							<th class={thCls}>Setor</th>
							{#each todosMeses as mes}
								<th class={thCls}>{abrevMes(mes)}</th>
							{/each}
							<th class={thCls}>Média</th>
							<th class={thCls}>Tendência</th>
						</tr>
					</thead>
					<tbody class="divide-y divide-border">
						{#each evolucao as linha (linha.setor.id)}
							<tr>
								<td class={cellCls}>
									<div class="font-medium">{linha.setor.nome}</div>
								</td>
								{#each todosMeses as mes}
									<td class={`${cellCls} tabnums`}>
										{formatarNota(linha.pontos.find((p) => p.mes === mes)?.nota ?? 0)}
									</td>
								{/each}
								<td class={`${cellCls} tabnums font-semibold`}>{formatarNota(linha.media)}</td>
								<td class={cellCls}>
									<span
										class={`inline-flex items-center gap-1 text-xs font-medium ${
											linha.tendencia === 'subindo'
												? 'text-success'
												: linha.tendencia === 'caindo'
													? 'text-danger'
													: 'text-muted'
										}`}
									>
										{linha.tendencia === 'subindo'
											? '↑ subindo'
											: linha.tendencia === 'caindo'
												? '↓ caindo'
												: '→ estável'}
									</span>
								</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>
		{/if}
	</div>
</section>