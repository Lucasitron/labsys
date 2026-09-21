<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type {
		EntradaResponse,
		ItemDetail,
		Loan,
		Movement,
		MovementSummary,
		PageInfo,
		SaidaResponse,
		Supplier,
		HistoryEntry,
		Tone
	} from '$lib/types/stock';
	import {
		entryKindMeta,
		exitReasonMeta,
		loanComputedMeta,
		quantityTone,
		categoriaMeta,
		localizacaoLabel,
		statusMeta
	} from '$lib/utils/stock-status';
	import { fmtMoney, fmtDate, fmtQty } from '$lib/utils/stock-format';
	import {
		listarEntradasPorItem,
		listarSaidasPorItem
	} from '$lib/api/stock/movements';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import Pagination from '$lib/components/ui/Pagination.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	import type { TipoSaida } from '$lib/types/stock';

	let { data }: PageProps = $props();
	const detail = $derived(data.detail);
	const loadError = $derived(data.error);

	let activeTab = $state('visao');
	const TABS: { id: string; label: string }[] = [
		{ id: 'visao', label: 'Visão geral' },
		{ id: 'movimentacoes', label: 'Movimentações' },
		{ id: 'emprestimos', label: 'Empréstimos' },
		{ id: 'fornecedores', label: 'Fornecedores' },
		{ id: 'historico', label: 'Histórico' }
	];

	const KEY_MOV = 'movimentacoes';
	const KEY_LOANS = 'emprestimos';
	const KEY_SUP = 'fornecedores';
	const KEY_HIST = 'historico';

	let tabLoaded = $state<Record<string, boolean>>({});
	let tabLoading = $state<Record<string, boolean>>({});
	let tabError = $state<Record<string, string | null>>({});

	let mov = $state<{
		summary: MovementSummary | null;
		movements: Movement[];
		pagination: PageInfo | null;
	} | null>(null);
	let movPage = $state(1);

	// Bloco 2 implementa os endpoints de empréstimos/fornecedores/histórico por item;
	// enquanto isso as abas mostram estado vazio (sem chamada de API).
	let loans = $state<Loan[]>([]);
	let suppliers = $state<Supplier[]>([]);
	let history = $state<HistoryEntry[]>([]);

	function toMovementIn(entrada: EntradaResponse, unidade: string): Movement {
		return {
			id: `e-${entrada.id}`,
			type: 'in',
			item: { id: entrada.idItem, name: entrada.nomeItem ?? 'Item', unit: unidade },
			quantity: entrada.quantidade,
			date: entrada.dataEntrada,
			origin: entrada.fornecedor?.nome ?? '—',
			reference: entrada.notaFiscal ?? undefined,
			unitValue: entrada.valorUnitario
		};
	}

	function toMovementOut(saida: SaidaResponse, unidade: string): Movement {
		return {
			id: `s-${saida.id}`,
			type: 'out',
			reason: saida.tipoSaida,
			item: { id: saida.idItem, name: saida.nomeItem ?? 'Item', unit: unidade },
			quantity: saida.quantidade,
			date: saida.dataSaida,
			destination: saida.idReferencia ?? '—',
			reference: saida.idReferencia ?? undefined
		};
	}

	async function loadMovements(page: number): Promise<void> {
		if (!detail || tabLoading[KEY_MOV]) return;
		tabLoading[KEY_MOV] = true;
		tabError[KEY_MOV] = null;
		movPage = page;
		try {
			const [entradas, saidas] = await Promise.all([
				listarEntradasPorItem(detail.id, fetch),
				listarSaidasPorItem(detail.id, fetch)
			]);
			const all: Movement[] = [
				...entradas.map((e) => toMovementIn(e, detail.unidadeMedida)),
				...saidas.map((s) => toMovementOut(s, detail.unidadeMedida))
			].sort((a, b) => b.date.localeCompare(a.date));

			const entriesCount = entradas.length;
			const exitsCount = saidas.length;
			const entriesSum = entradas.reduce((acc, e) => acc + e.quantidade, 0);
			const exitsSum = saidas.reduce((acc, s) => acc + s.quantidade, 0);

			const pageSize = 10;
			const totalItems = all.length;
			const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
			const safePage = Math.min(Math.max(1, page), totalPages);
			const start = (safePage - 1) * pageSize;

			mov = {
				summary: {
					entries: { count: entriesCount, sum: entriesSum },
					exits: { count: exitsCount, sum: exitsSum }
				},
				movements: all.slice(start, start + pageSize),
				pagination: { page: safePage, pageSize, totalItems, totalPages }
			};
			tabLoaded[KEY_MOV] = true;
		} catch (err) {
			tabError[KEY_MOV] = err instanceof Error ? err.message : 'Erro ao carregar movimentações';
		} finally {
			tabLoading[KEY_MOV] = false;
		}
	}

	async function activate(tab: string): Promise<void> {
		if (!detail) return;
		activeTab = tab;

		if (tab === KEY_MOV) {
			if (!tabLoaded[KEY_MOV] && !tabLoading[KEY_MOV]) await loadMovements(1);
		} else if (tab === KEY_LOANS && !tabLoaded[KEY_LOANS]) {
			tabLoaded[KEY_LOANS] = true;
		} else if (tab === KEY_SUP && !tabLoaded[KEY_SUP]) {
			tabLoaded[KEY_SUP] = true;
		} else if (tab === KEY_HIST && !tabLoaded[KEY_HIST]) {
			tabLoaded[KEY_HIST] = true;
		}
	}

	function retryTab(tab: string): void {
		tabError[tab] = null;
		tabLoading[tab] = false;
		if (tab === KEY_MOV) {
			tabLoaded[KEY_MOV] = false;
			void loadMovements(movPage);
		}
	}

	// ---- meta helpers ----

	const TONE_TEXT: Record<Tone, string> = {
		success: 'text-success',
		warn: 'text-warn',
		danger: 'text-danger',
		brand: 'text-brandhi',
		muted: 'text-muted',
		ink: 'text-ink'
	};

	function qtyClass(current: number, minimum: number): string {
		return TONE_TEXT[quantityTone(current, minimum)];
	}

	function movementMeta(m: Movement): { label: string; color: Tone } {
		if (m.type === 'in' && m.kind) return entryKindMeta(m.kind);
		if (m.type === 'out' && m.reason) return exitReasonMeta(m.reason);
		return {
			label: m.type === 'in' ? 'Entrada' : 'Saída',
			color: m.type === 'in' ? 'success' : 'danger'
		};
	}

	function categorias(detail: ItemDetail): string {
		return categoriaMeta(detail.categoria).label;
	}
</script>

<svelte:head>
	<title>{detail?.nome ?? 'Item'} — Estoque — FabLab</title>
</svelte:head>

{#if loadError && !detail}
	<ErrorBanner message="Não foi possível carregar o item" hint={loadError} onRetry={invalidateAll} />
{:else if !detail}
	<!-- Loading -->
	<div class="space-y-4">
		<div class="rounded-xl border border-border bg-surface p-6">
			<Skeleton class="h-5 w-40" />
			<Skeleton class="mt-3 h-8 w-64" />
			<Skeleton class="mt-2 h-3 w-72 opacity-60" />
		</div>
		<div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
			{#each [0, 1, 2, 3] as i (i)}
				<div class="rounded-xl border border-border bg-surface p-4">
					<Skeleton class="h-3 w-20" />
					<Skeleton class="mt-2 h-7 w-14" />
					<Skeleton class="mt-2 h-2 w-24 opacity-60" />
				</div>
			{/each}
		</div>
		<div class="rounded-xl border border-border bg-surface p-6">
			<TableSkeleton rows={6} />
		</div>
	</div>
{:else}
	<!-- Barra de ações -->
	<div class="flex items-center justify-between gap-4">
		<a
			href="/estoque/itens"
			class="inline-flex items-center gap-2 text-xs font-medium text-muted transition-colors hover:text-ink"
		>
			<Icon name="chevron-left" class="h-3.5 w-3.5" /> Voltar para itens
		</a>
	</div>

	<!-- Hero -->
	<section class="rounded-xl border border-border bg-surface p-6">
		<div class="flex flex-wrap gap-5">
			<div
				class="flex h-24 w-24 shrink-0 items-center justify-center rounded-xl border border-border bg-elevated"
			>
				<Icon name="photo" class="h-10 w-10 text-muted/60" />
			</div>
			<div class="min-w-0 flex-1">
				<h1 class="mt-0.5 truncate text-2xl font-semibold tracking-tight text-ink">
					{detail.nome}
				</h1>
				<p class="mt-1 text-sm text-muted">
					{categorias(detail)} · {detail.unidadeMedida} ·
					{localizacaoLabel(detail.localizacao)}
				</p>
			</div>
			<StatusBadge {...statusMeta(detail.status)} />
		</div>
	</section>

	<!-- KPIs -->
	<div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
		<div class="rounded-xl border border-border bg-surface p-4">
			<span class="text-xs text-muted">Qtd. atual</span>
			<div class="mt-1 flex items-baseline gap-1.5">
				<span
					class="text-2xl font-semibold tracking-tight {qtyClass(detail.quantidadeAtual, detail.estoqueMinimo)}"
				>
					{fmtQty(detail.quantidadeAtual)}
				</span>
			</div>
			<p class="mt-1 text-[11px] text-muted">
				{detail.quantidadeAtual < detail.estoqueMinimo ? 'abaixo do mínimo' : 'dentro do esperado'}
			</p>
		</div>

		<div class="rounded-xl border border-border bg-surface p-4">
			<span class="text-xs text-muted">Qtd. mínima</span>
			<div class="mt-1 flex items-baseline gap-1.5">
				<span class="text-2xl font-semibold tracking-tight">
					{fmtQty(detail.estoqueMinimo)}
				</span>
			</div>
			<p class="mt-1 text-[11px] text-muted">em {detail.unidadeMedida}</p>
		</div>

		<div class="rounded-xl border border-border bg-surface p-4">
			<span class="text-xs text-muted">Empréstimos</span>
			<div class="mt-1 flex items-baseline gap-1.5">
				<span class="text-2xl font-semibold tracking-tight">
					{detail.activeLoans ?? '—'}
				</span>
				{#if detail.activeLoans !== undefined}<span class="text-xs text-muted">ativos</span>{/if}
			</div>
		</div>

		<div class="rounded-xl border border-border bg-surface p-4">
			<span class="text-xs text-muted">Última entrada</span>
			<div class="mt-1 flex items-baseline gap-1.5">
				<span class="text-2xl font-semibold tracking-tight">
					{detail.lastEntry ? fmtDate(detail.lastEntry.at) : '—'}
				</span>
			</div>
		</div>
	</div>

	<!-- Abas -->
	<section class="overflow-hidden rounded-xl border border-border bg-surface">
		<div class="flex items-center gap-1 overflow-x-auto border-b border-border px-2" role="tablist">
			{#each TABS as tab (tab.id)}
				<button
					role="tab"
					aria-selected={activeTab === tab.id}
					onclick={() => activate(tab.id)}
					class="whitespace-nowrap border-b-2 px-4 py-3 text-sm font-medium transition-colors {activeTab === tab.id
						? 'border-brand text-brand'
						: 'border-transparent text-muted hover:text-ink'}"
				>
					{tab.label}
				</button>
			{/each}
		</div>

		<div class="p-6">
			{#if activeTab === 'visao'}
				<div class="space-y-8">
					{#if detail.descricao}
						<div>
							<h2 class="mb-2 text-xs font-medium uppercase tracking-wide text-muted">
								Descrição
							</h2>
							<p class="text-sm leading-relaxed text-ink">{detail.descricao}</p>
						</div>
					{/if}

					<div>
						<h2 class="mb-3 text-xs font-medium uppercase tracking-wide text-muted">
							Especificações
						</h2>
						<dl
							class="grid grid-cols-1 divide-y divide-border/50 overflow-hidden rounded-lg border border-border sm:grid-cols-[220px_1fr]"
						>
							{@render specRow('Categoria', categorias(detail))}
							{@render specRow('Unidade', detail.unidadeMedida)}
							{@render specRow('Localização', localizacaoLabel(detail.localizacao))}
							{@render specRow(
								'Estoque mínimo',
								fmtQty(detail.estoqueMinimo, detail.unidadeMedida)
							)}
							{@render specRow('Estoque máximo', detail.maximum != null ? fmtQty(detail.maximum, detail.unidadeMedida) : '—')}
							{@render specRow('Valor unitário', fmtMoney(detail.unitValue))}
						</dl>
					</div>

					{#if detail.bomUsage && detail.bomUsage.length > 0}
						<div>
							<h2 class="mb-3 text-xs font-medium uppercase tracking-wide text-muted">
								Usado em (BOM)
							</h2>
							<div class="divide-y divide-border border border-border rounded-lg">
								{#each detail.bomUsage as use (use.projectId)}
									<div class="flex items-center justify-between px-4 py-2.5">
										<span class="text-sm text-ink">{use.projectName}</span>
										<span class="font-mono text-sm text-muted">
											{use.qty} {use.unit}/unidade
										</span>
									</div>
								{/each}
							</div>
						</div>
					{/if}
				</div>

			{:else if activeTab === 'movimentacoes'}
				{#if tabLoading[KEY_MOV] && !mov}
					<TableSkeleton rows={5} />
				{:else if tabError[KEY_MOV] && !mov}
					<ErrorBanner
						message="Movimentações indisponíveis"
						hint={tabError[KEY_MOV]}
						onRetry={() => retryTab(KEY_MOV)}
					/>
				{:else if mov && mov.movements.length === 0}
					<EmptyState
						icon="box"
						title="Sem movimentações"
						description="Este item ainda não registrou entradas ou saídas."
					/>
				{:else if mov}
					<div class="space-y-4">
						<div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
							<div class="rounded-lg border border-success/30 bg-success/5 px-4 py-3">
								<span class="text-xs text-muted">Entradas</span>
								<p class="mt-0.5 text-lg font-semibold text-success">
									+{mov.summary?.entries?.count ?? 0} · {fmtQty(
										mov.summary?.entries?.sum ?? 0,
										detail.unidadeMedida
									)}
								</p>
							</div>
							<div class="rounded-lg border border-danger/30 bg-danger/5 px-4 py-3">
								<span class="text-xs text-muted">Saídas</span>
								<p class="mt-0.5 text-lg font-semibold text-danger">
									−{mov.summary?.exits?.count ?? 0} · {fmtQty(
										mov.summary?.exits?.sum ?? 0,
										detail.unidadeMedida
									)}
								</p>
							</div>
						</div>

						<div class="overflow-x-auto">
							<table class="w-full text-sm">
								<thead>
									<tr
										class="border-b border-border text-left text-[11px] uppercase tracking-wide text-muted"
									>
										<th class="px-4 py-2.5 font-medium">Data</th>
										<th class="px-4 py-2.5 font-medium">Tipo</th>
										<th class="px-4 py-2.5 text-right font-medium">Qtd</th>
										<th class="px-4 py-2.5 font-medium">Referência</th>
									</tr>
								</thead>
								<tbody class="divide-y divide-border">
									{#each mov.movements as m (m.id)}
										{@const meta = movementMeta(m)}
										<tr>
											<td class="px-4 py-3 text-muted">{fmtDate(m.date)}</td>
											<td class="px-4 py-3">
												<StatusBadge label={meta.label} color={meta.color} />
											</td>
											<td
												class="px-4 py-3 text-right font-mono {m.type === 'in' ? 'text-success' : 'text-danger'}"
											>
												{m.type === 'in' ? '+' : '−'}{fmtQty(m.quantity, m.item.unit)}
											</td>
											<td class="px-4 py-3 text-muted">{m.reference ?? '—'}</td>
										</tr>
									{/each}
								</tbody>
							</table>
						</div>

						<div class="border-t border-border pt-3">
							<Pagination
								page={movPage}
								totalPages={mov.pagination?.totalPages ?? 0}
								totalItems={mov.pagination?.totalItems}
								onPage={loadMovements}
								label="movimentações"
							/>
						</div>
					</div>
				{/if}

			{:else if activeTab === 'emprestimos'}
				<EmptyState
					icon="arrow-uturn-left"
					title="Sem empréstimos"
					description="Nenhum empréstimo registrado para este item."
				/>

			{:else if activeTab === 'fornecedores'}
				{#if tabLoading[KEY_SUP] && suppliers === null}
					<TableSkeleton rows={5} />
				{:else if tabError[KEY_SUP] && suppliers === null}
					<ErrorBanner
						message="Fornecedores indisponíveis"
						hint={tabError[KEY_SUP]}
						onRetry={() => retryTab(KEY_SUP)}
					/>
				{:else if suppliers && suppliers.length === 0}
					<EmptyState
						icon="truck"
						title="Sem fornecedores"
						description="Nenhum fornecedor vinculado a este item."
					/>
				{/if}

			{:else if activeTab === 'historico'}
				<EmptyState
					icon="clock"
					title="Sem histórico"
					description="Nenhuma alteração registrada para este item."
				/>
			{/if}
		</div>
	</section>
{/if}

{#snippet specRow(label: string, value: string, extra = '')}
	<div class="contents">
		<dt class="bg-elevated/40 px-4 py-2.5 text-xs text-muted">{label}</dt>
		<dd class="px-4 py-2.5 text-sm {extra}">{value}</dd>
	</div>
{/snippet}