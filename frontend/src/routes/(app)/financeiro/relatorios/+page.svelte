<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type {
		CustoMaquina,
		DoacoesDespesas,
		Dre,
		FluxoCaixa,
		Inadimplencia,
		Lucratividade,
		RelatorioId
	} from '$lib/types/financeiro';
	import { formatDateBR, formatMoneyBRL } from '$lib/utils/vendas-format';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import Select from '$lib/components/ui/Select.svelte';

	let { data }: PageProps = $props();

	const relatorio = $derived<RelatorioId>(data.relatorio);
	const periodo = $derived<string>(data.periodo);
	const dados = $derived(data.dados);
	const erro = $derived(data.error);

	const carregando = $derived(dados === null && erro === null);

	interface RelatorioOpcao {
		id: RelatorioId;
		label: string;
		descricao: string;
		icone: 'chart' | 'document' | 'star' | 'warning' | 'gift' | 'cube';
		tom: string;
	}

	const RELATORIOS: RelatorioOpcao[] = [
		{
			id: 'fluxo',
			label: 'Fluxo de caixa',
			descricao: 'Entradas, saídas e líquido por semana',
			icone: 'chart',
			tom: 'text-brandhi'
		},
		{
			id: 'dre',
			label: 'DRE',
			descricao: 'Receitas, custos e resultado do período',
			icone: 'document',
			tom: 'text-warn'
		},
		{
			id: 'lucratividade',
			label: 'Lucratividade',
			descricao: 'Margem por encomenda',
			icone: 'star',
			tom: 'text-success'
		},
		{
			id: 'inadimplencia',
			label: 'Inadimplência',
			descricao: 'Recebíveis vencidos',
			icone: 'warning',
			tom: 'text-danger'
		},
		{
			id: 'doacoes',
			label: 'Doações vs. despesas',
			descricao: 'Saldo mensal e acumulado',
			icone: 'gift',
			tom: 'text-brandhi'
		},
		{
			id: 'custo-maquina',
			label: 'Custo por máquina',
			descricao: 'Horas, custo e participação',
			icone: 'cube',
			tom: 'text-muted'
		}
	];

	const PERIODO_OPCOES = [
		{ id: '2026-09', label: 'Setembro 2026' },
		{ id: '2026-08', label: 'Agosto 2026' },
		{ id: '2026-07', label: 'Julho 2026' },
		{ id: '2026-06', label: 'Junho 2026' },
		{ id: '2026-2tri', label: '2º tri 2026' },
		{ id: '2026', label: 'Ano 2026' }
	];

	function navegar(rel: string, per: string): void {
		const url = new URLSearchParams();
		url.set('relatorio', rel);
		url.set('periodo', per);
		void goto(`/financeiro/relatorios?${url.toString()}`);
	}

	function trocarRelatorio(id: RelatorioId): void {
		navegar(id, periodo);
	}

	function trocarPeriodo(valor: string): void {
		if (valor) navegar(relatorio, valor);
	}

	function tentarNovamente(): void {
		void invalidateAll();
	}

	const fluxo = $derived(relatorio === 'fluxo' ? (dados as FluxoCaixa | null) : null);
	const dre = $derived(relatorio === 'dre' ? (dados as Dre | null) : null);
	const lucratividade = $derived(
		relatorio === 'lucratividade' ? (dados as Lucratividade | null) : null
	);
	const inadimplencia = $derived(
		relatorio === 'inadimplencia' ? (dados as Inadimplencia | null) : null
	);
	const doacoes = $derived(relatorio === 'doacoes' ? (dados as DoacoesDespesas | null) : null);
	const custoMaquina = $derived(
		relatorio === 'custo-maquina' ? (dados as CustoMaquina | null) : null
	);
</script>

<svelte:head>
	<title>Relatórios — Financeiro — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Relatórios"
		subtitle="Saúde financeira do laboratório — dados servidos pelo backend, sem recálculo."
	>
		{#snippet children()}
			<div class="min-w-48">
				<Select
					id="rel-periodo"
					label="Período"
					options={PERIODO_OPCOES}
					value={periodo}
					onChange={trocarPeriodo}
					placeholder="Período…"
				/>
			</div>
		{/snippet}
	</PageHeader>

	<div
		role="group"
		aria-label="Seleção de relatório"
		class="grid gap-2 sm:grid-cols-2 lg:grid-cols-3"
	>
		{#each RELATORIOS as r (r.id)}
			{@const ativo = relatorio === r.id}
			<button
				type="button"
				data-testid="rel-btn-{r.id}"
				onclick={() => trocarRelatorio(r.id)}
				aria-label="Ver relatório {r.label}"
				aria-pressed={ativo}
				class="rel-btn flex items-start gap-3 rounded-xl border p-4 text-left transition {ativo
					? 'border-brand bg-brand/10'
					: 'border-border bg-surface hover:border-brand/40'}"
			>
				<span class="{r.tom} mt-0.5" aria-hidden="true">
					<Icon name={r.icone} class="h-5 w-5" />
				</span>
				<span class="min-w-0">
					<span class="block text-sm font-semibold text-ink">{r.label}</span>
					<span class="mt-0.5 block text-xs text-muted">{r.descricao}</span>
				</span>
			</button>
		{/each}
	</div>

	{#if erro}
		<ErrorBanner
			message="Não foi possível carregar o relatório"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={tentarNovamente}
			testid="rel-retry"
		/>
	{:else if carregando}
		<div class="rounded-xl border border-border bg-surface p-4" aria-hidden="true">
			<TableSkeleton rows={6} columns={4} />
		</div>
	{:else if relatorio === 'fluxo'}
		<section
			aria-label="Resultado do fluxo de caixa"
			class="overflow-hidden rounded-xl border border-border bg-surface"
		>
			{#if !fluxo || fluxo.linhas.length === 0}
				<EmptyState
					icon="chart"
					title="Sem dados de fluxo no período"
					description="Não há movimentação servida para o período selecionado."
				/>
			{:else}
				<div class="overflow-x-auto" data-testid="rel-table">
					<table class="w-full min-w-[560px] text-sm">
						<thead>
							<tr
								class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
							>
								<th class="px-4 py-2.5 font-medium">Semana</th>
								<th class="px-4 py-2.5 text-right font-medium">Entradas</th>
								<th class="px-4 py-2.5 text-right font-medium">Saídas</th>
								<th class="px-4 py-2.5 text-right font-medium">Líquido</th>
							</tr>
						</thead>
						<tbody>
							{#each fluxo.linhas as l (l.periodo)}
								<tr class="border-b border-border transition last:border-0 hover:bg-elevated/40">
									<td class="px-4 py-3 font-medium text-ink">{l.periodo}</td>
									<td class="px-4 py-3 text-right font-mono text-sm text-success tabular-nums">
										{formatMoneyBRL(l.entradas)}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm text-danger tabular-nums">
										{formatMoneyBRL(l.saidas)}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm text-ink tabular-nums">
										{formatMoneyBRL(l.liquido)}
									</td>
								</tr>
							{/each}
						</tbody>
						<tfoot>
							<tr class="border-t border-border bg-elevated/50">
								<td class="px-4 py-3 text-sm font-semibold text-ink">Total</td>
								<td class="px-4 py-3"></td>
								<td class="px-4 py-3"></td>
								<td class="px-4 py-3 text-right font-mono text-sm font-semibold text-ink tabular-nums">
									{formatMoneyBRL(fluxo.total)}
								</td>
							</tr>
						</tfoot>
					</table>
				</div>
			{/if}
		</section>
	{:else if relatorio === 'dre'}
		<section
			aria-label="Resultado da DRE"
			class="rounded-xl border border-border bg-surface p-5"
			data-testid="rel-table"
		>
			{#if !dre}
				<EmptyState
					icon="document"
					title="Sem dados de DRE no período"
					description="Nenhum demonstrativo servido para o período selecionado."
				/>
			{:else}
				<dl class="space-y-2.5 text-sm">
					<div class="flex items-center justify-between gap-2">
						<dt class="text-muted">Receita operacional (+)</dt>
						<dd class="font-mono font-medium text-success tabular-nums">
							{formatMoneyBRL(dre.receitaOperacional)}
						</dd>
					</div>
					<div class="flex items-center justify-between gap-2">
						<dt class="text-muted">Custos diretos (−)</dt>
						<dd class="font-mono font-medium text-danger tabular-nums">
							{formatMoneyBRL(dre.custosDiretos)}
						</dd>
					</div>
					<div class="flex items-center justify-between gap-2">
						<dt class="text-muted">Despesas operacionais (−)</dt>
						<dd class="font-mono font-medium text-danger tabular-nums">
							{formatMoneyBRL(dre.despesasOperacionais)}
						</dd>
					</div>
					<div class="flex items-center justify-between gap-2">
						<dt class="text-muted">Doações e recursos (+)</dt>
						<dd class="font-mono font-medium text-success tabular-nums">
							{formatMoneyBRL(dre.doacoesRecursos)}
						</dd>
					</div>
					<div
						class="flex items-center justify-between gap-2 border-t border-border pt-2.5"
					>
						<dt class="font-semibold text-ink">Resultado do período</dt>
						<dd class="font-mono font-semibold text-ink tabular-nums">
							{formatMoneyBRL(dre.resultado)}
						</dd>
					</div>
				</dl>
			{/if}
		</section>
	{:else if relatorio === 'lucratividade'}
		<section
			aria-label="Resultado da lucratividade"
			class="overflow-hidden rounded-xl border border-border bg-surface"
		>
			{#if !lucratividade || lucratividade.linhas.length === 0}
				<EmptyState
					icon="star"
					title="Sem dados de lucratividade no período"
					description="Nenhuma encomenda servida para o período selecionado."
				/>
			{:else}
				<div class="overflow-x-auto" data-testid="rel-table">
					<table class="w-full min-w-[640px] text-sm">
						<thead>
							<tr
								class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
							>
								<th class="px-4 py-2.5 font-medium">Encomenda</th>
								<th class="px-4 py-2.5 text-right font-medium">Valor venda</th>
								<th class="px-4 py-2.5 text-right font-medium">Custo total</th>
								<th class="px-4 py-2.5 text-right font-medium">Margem (R$)</th>
								<th class="px-4 py-2.5 text-right font-medium">Margem (%)</th>
							</tr>
						</thead>
						<tbody>
							{#each lucratividade.linhas as l (l.idEncomenda)}
								<tr class="border-b border-border transition last:border-0 hover:bg-elevated/40">
									<td class="px-4 py-3 font-mono text-xs text-ink">{l.idEncomenda}</td>
									<td class="px-4 py-3 text-right font-mono text-sm text-ink tabular-nums">
										{formatMoneyBRL(l.valorVenda)}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm text-ink tabular-nums">
										{formatMoneyBRL(l.custoTotal)}
									</td>
									<td
										class="px-4 py-3 text-right font-mono text-sm tabular-nums {l.margemReais < 0
											? 'text-danger'
											: 'text-success'}"
									>
										{formatMoneyBRL(l.margemReais)}
									</td>
									<td
										class="px-4 py-3 text-right font-mono text-sm tabular-nums {l.margemPercentual < 0
											? 'text-danger'
											: 'text-ink'}"
									>
										{new Intl.NumberFormat('pt-BR', {
											maximumFractionDigits: 1
										}).format(l.margemPercentual)}%
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
			{/if}
		</section>
	{:else if relatorio === 'inadimplencia'}
		<section
			aria-label="Resultado da inadimplência"
			class="overflow-hidden rounded-xl border border-border bg-surface"
		>
			{#if !inadimplencia || inadimplencia.linhas.length === 0}
				<EmptyState
					icon="check"
					title="Sem inadimplência no período"
					description="Nenhum recebível vencido servido — carteira em dia ✓"
				/>
			{:else}
				<div class="overflow-x-auto" data-testid="rel-table">
					<table class="w-full min-w-[680px] text-sm">
						<thead>
							<tr
								class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
							>
								<th class="px-4 py-2.5 font-medium">Cliente</th>
								<th class="px-4 py-2.5 font-medium">Referência</th>
								<th class="px-4 py-2.5 font-medium">Vencimento</th>
								<th class="px-4 py-2.5 text-right font-medium">Dias em atraso</th>
								<th class="px-4 py-2.5 text-right font-medium">Valor</th>
							</tr>
						</thead>
						<tbody>
							{#each inadimplencia.linhas as l (`${l.cliente}-${l.referencia}`)}
								<tr
									class="border-b border-border bg-danger/[0.03] transition last:border-0 hover:bg-elevated/40"
								>
									<td class="px-4 py-3 font-medium text-ink">{l.cliente}</td>
									<td class="px-4 py-3 font-mono text-xs text-muted">{l.referencia}</td>
									<td class="px-4 py-3 font-mono text-xs text-danger tabular-nums">
										{formatDateBR(l.vencimento)}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm text-danger tabular-nums">
										{l.diasAtraso}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm text-ink tabular-nums">
										{formatMoneyBRL(l.valor)}
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
			{/if}
		</section>
	{:else if relatorio === 'doacoes'}
		<section
			aria-label="Resultado de doações versus despesas"
			class="overflow-hidden rounded-xl border border-border bg-surface"
		>
			{#if !doacoes || doacoes.linhas.length === 0}
				<EmptyState
					icon="gift"
					title="Sem dados de doações no período"
					description="Nenhum comparativo servido para o período selecionado."
				/>
			{:else}
				<div class="overflow-x-auto" data-testid="rel-table">
					<table class="w-full min-w-[560px] text-sm">
						<thead>
							<tr
								class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
							>
								<th class="px-4 py-2.5 font-medium">Mês</th>
								<th class="px-4 py-2.5 text-right font-medium">Doações</th>
								<th class="px-4 py-2.5 text-right font-medium">Despesas</th>
								<th class="px-4 py-2.5 text-right font-medium">Saldo</th>
							</tr>
						</thead>
						<tbody>
							{#each doacoes.linhas as l (l.mes)}
								<tr class="border-b border-border transition last:border-0 hover:bg-elevated/40">
									<td class="px-4 py-3 font-medium text-ink">{l.mes}</td>
									<td class="px-4 py-3 text-right font-mono text-sm text-success tabular-nums">
										{formatMoneyBRL(l.doacoes)}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm text-danger tabular-nums">
										{formatMoneyBRL(l.despesas)}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm text-ink tabular-nums">
										{formatMoneyBRL(l.saldo)}
									</td>
								</tr>
							{/each}
						</tbody>
						<tfoot>
							<tr class="border-t border-border bg-elevated/50">
								<td class="px-4 py-3 text-sm font-semibold text-ink">Acumulado</td>
								<td class="px-4 py-3"></td>
								<td class="px-4 py-3"></td>
								<td class="px-4 py-3 text-right font-mono text-sm font-semibold text-ink tabular-nums">
									{formatMoneyBRL(doacoes.acumulado)}
								</td>
							</tr>
						</tfoot>
					</table>
				</div>
			{/if}
		</section>
	{:else}
		<section
			aria-label="Resultado do custo por máquina"
			class="overflow-hidden rounded-xl border border-border bg-surface"
		>
			{#if !custoMaquina || custoMaquina.linhas.length === 0}
				<EmptyState
					icon="cube"
					title="Sem dados de custo por máquina no período"
					description="Nenhum apontamento servido para o período selecionado."
				/>
			{:else}
				<div class="overflow-x-auto" data-testid="rel-table">
					<table class="w-full min-w-[560px] text-sm">
						<thead>
							<tr
								class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
							>
								<th class="px-4 py-2.5 font-medium">Máquina</th>
								<th class="px-4 py-2.5 text-right font-medium">Horas</th>
								<th class="px-4 py-2.5 text-right font-medium">Custo</th>
								<th class="px-4 py-2.5 text-right font-medium">% do total</th>
							</tr>
						</thead>
						<tbody>
							{#each custoMaquina.linhas as l (l.maquina)}
								<tr class="border-b border-border transition last:border-0 hover:bg-elevated/40">
									<td class="px-4 py-3 font-medium text-ink">{l.maquina}</td>
									<td class="px-4 py-3 text-right font-mono text-sm text-ink tabular-nums">
										{new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 }).format(l.horas)} h
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm text-ink tabular-nums">
										{formatMoneyBRL(l.custo)}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm text-muted tabular-nums">
										{new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 }).format(
											l.percentual
										)}%
									</td>
								</tr>
							{/each}
						</tbody>
						<tfoot>
							<tr class="border-t border-border bg-elevated/50">
								<td class="px-4 py-3 text-sm font-semibold text-ink">Total</td>
								<td class="px-4 py-3"></td>
								<td
									class="px-4 py-3 text-right font-mono text-sm font-semibold text-ink tabular-nums"
								>
									{formatMoneyBRL(custoMaquina.total)}
								</td>
								<td class="px-4 py-3"></td>
							</tr>
						</tfoot>
					</table>
				</div>
			{/if}
		</section>
	{/if}
</div>
