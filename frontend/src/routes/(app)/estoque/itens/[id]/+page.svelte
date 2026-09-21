<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type {
		EntradaResponse,
		ItemDetail,
		Movement,
		MovementSummary,
		PageInfo,
		SaidaResponse,
		Tone
	} from '$lib/types/stock';
	import {
		exitReasonMeta,
		quantityTone,
		categoriaMeta,
		localizacaoLabel,
		statusMeta
	} from '$lib/utils/stock-status';
	import { fmtDate, fmtQty } from '$lib/utils/stock-format';
	import {
		listarEntradasPorItem,
		listarSaidasPorItem
	} from '$lib/api/stock/movements';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import Pagination from '$lib/components/ui/Pagination.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();
	const canEdit = $derived(data.canEdit ?? false);
	const detail = $derived(data.detail);
	const loadError = $derived(data.error);

	const TABS: { id: string; label: string }[] = [
		{ id: 'visao', label: 'Visão geral' },
		{ id: 'movimentacoes', label: 'Movimentações' },
		{ id: 'emprestimos', label: 'Empréstimos' },
		{ id: 'fornecedores', label: 'Fornecedores' },
		{ id: 'historico', label: 'Histórico' }
	];

	const KEY_MOV = 'movimentacoes';
	const KEY_SUP = 'fornecedores';

	// Abas via URL (?tab=) — URL como fonte de verdade.
	const VALID_TABS = TABS.map((t) => t.id);
	const activeTab = $derived(VALID_TABS.includes(data.tab) ? data.tab : 'visao');

	function selectTab(id: string): void {
		void goto(`/estoque/itens/${detail?.id}?tab=${id}`, { invalidateAll: false });
	}

	let tabLoaded = $state<Record<string, boolean>>({});
	let tabLoading = $state<Record<string, boolean>>({});
	let tabError = $state<Record<string, string | null>>({});

	let mov = $state<{
		summary: MovementSummary | null;
		movements: LinhaMov[];
		pagination: PageInfo | null;
	} | null>(null);
	let movPage = $state(1);

	interface LinhaMov extends Movement {
		rotaId: string;
		rotaTipo: 'entrada' | 'saida';
	}

	function toMovementIn(entrada: EntradaResponse, unidade: string): LinhaMov {
		return {
			id: `e-${entrada.id}`,
			rotaId: entrada.id,
			rotaTipo: 'entrada',
			type: 'in',
			item: { id: entrada.idItem, name: entrada.nomeItem ?? 'Item', unit: unidade },
			quantity: entrada.quantidade,
			date: entrada.dataEntrada,
			origin: entrada.fornecedor?.nome ?? '—',
			reference: entrada.notaFiscal ?? undefined,
			unitValue: entrada.valorUnitario
		};
	}

	function toMovementOut(saida: SaidaResponse, unidade: string): LinhaMov {
		return {
			id: `s-${saida.id}`,
			rotaId: saida.id,
			rotaTipo: 'saida',
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
			const all: LinhaMov[] = [
				...entradas.map((e) => toMovementIn(e, detail.unidadeMedida)),
				...saidas.map((s) => toMovementOut(s, detail.unidadeMedida))
			].sort((a, b) => b.date.localeCompare(a.date));

			const pageSize = 10;
			const totalItems = all.length;
			const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
			const safePage = Math.min(Math.max(1, page), totalPages);
			const start = (safePage - 1) * pageSize;

			mov = {
				summary: {
					entries: { count: entradas.length, sum: entradas.reduce((a, e) => a + e.quantidade, 0) },
					exits: { count: saidas.length, sum: saidas.reduce((a, s) => a + s.quantidade, 0) }
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

	function retryTab(tab: string): void {
		tabError[tab] = null;
		tabLoading[tab] = false;
		if (tab === KEY_MOV) {
			tabLoaded[KEY_MOV] = false;
			void loadMovements(movPage);
		}
	}

	function abrirMovimentacao(m: LinhaMov): void {
		const base = m.rotaTipo === 'entrada' ? '/estoque/entradas/' : '/estoque/saidas/';
		void goto(`${base}${m.rotaId}`);
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

	function movementMeta(m: LinhaMov): { label: string; color: Tone } {
		if (m.type === 'out' && m.reason) return exitReasonMeta(m.reason);
		return { label: 'Entrada', color: 'success' };
	}

	function categoriaLabel(detail: ItemDetail): string {
		return categoriaMeta(detail.categoria).label;
	}
</script>

<svelte:head>
	<title>{detail?.nome ?? 'Item'} — Estoque — FabLab</title>
</svelte:head>

{#if loadError && !detail}
	<div data-testid="it-detalhe-error">
		<ErrorBanner
			message="Não foi possível carregar o item"
			hint={loadError}
			onRetry={invalidateAll}
		/>
	</div>
{:else if !detail}
	<!-- Loading -->
	<div class="space-y-4" data-testid="it-detalhe-loading">
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
	<div data-testid="it-detalhe" class="space-y-4">
		<!-- Barra de ações -->
		<div class="flex items-center justify-between gap-4">
			<a
				href="/estoque/itens"
				class="inline-flex items-center gap-2 text-xs font-medium text-muted transition-colors hover:text-ink"
			>
				<Icon name="chevron-left" class="h-3.5 w-3.5" /> Voltar para itens
			</a>
			{#if canEdit}
				<a
					data-testid="it-detalhe-editar"
					href={`/estoque/itens/${detail.id}/editar`}
					class="inline-flex items-center gap-1.5 rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:border-brand/50 hover:text-brandhi"
				>
					<Icon name="pencil" class="h-4 w-4" /> Editar
				</a>
			{/if}
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
						{categoriaLabel(detail)} · {detail.unidadeMedida} ·
						{localizacaoLabel(detail.localizacao)}
					</p>
				</div>
				<StatusBadge {...statusMeta(detail.status)} />
			</div>
		</section>

		<!-- KPIs reais (derivados do backend) -->
		<div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
			<div class="rounded-xl border border-border bg-surface p-4">
				<span class="text-xs text-muted">Qtd. atual</span>
				<div class="mt-1 flex items-baseline gap-1.5">
					<span
						class="text-2xl font-semibold tracking-tight {qtyClass(detail.quantidadeAtual, detail.estoqueMinimo)}"
					>
						{fmtQty(detail.quantidadeAtual, detail.unidadeMedida)}
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
						{fmtQty(detail.estoqueMinimo, detail.unidadeMedida)}
					</span>
				</div>
				<p class="mt-1 text-[11px] text-muted">em {detail.unidadeMedida}</p>
			</div>

			<div class="rounded-xl border border-border bg-surface p-4">
				<span class="text-xs text-muted">Unidade de medida</span>
				<div class="mt-1 flex items-baseline gap-1.5">
					<span class="text-2xl font-semibold tracking-tight">
						{detail.unidadeMedida || '—'}
					</span>
				</div>
				<p class="mt-1 text-[11px] text-muted">unidade de referência</p>
			</div>

			<div class="rounded-xl border border-border bg-surface p-4">
				<span class="text-xs text-muted">Localização</span>
				<div class="mt-1 flex items-baseline gap-1.5">
					<span class="text-lg font-semibold tracking-tight">
						{localizacaoLabel(detail.localizacao)}
					</span>
				</div>
				<p class="mt-1 text-[11px] text-muted">
					{detail.localizacao ? `A: ${detail.localizacao.armario}` : 'sem localização definida'}
				</p>
			</div>
		</div>

		<!-- Abas -->
		<section class="overflow-hidden rounded-xl border border-border bg-surface">
			<div class="flex items-center gap-1 overflow-x-auto border-b border-border px-2" role="tablist">
				{#each TABS as tab (tab.id)}
					<button
						data-testid="it-tab-{tab.id}"
						role="tab"
						aria-selected={activeTab === tab.id}
						onclick={() => selectTab(tab.id)}
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
								{@render specRow('Categoria', categoriaLabel(detail))}
								{@render specRow('Unidade', detail.unidadeMedida)}
								{@render specRow('Localização', localizacaoLabel(detail.localizacao))}
								{@render specRow(
									'Estoque mínimo',
									fmtQty(detail.estoqueMinimo, detail.unidadeMedida)
								)}
								{@render specRow(
									'Estoque atual',
									fmtQty(detail.quantidadeAtual, detail.unidadeMedida)
								)}
							</dl>
						</div>
					</div>

				{:else if activeTab === 'movimentacoes'}
					<div data-testid="it-tab-movimentacoes">
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
												<tr
													data-testid="mv-linha"
													onclick={() => abrirMovimentacao(m)}
													class="cursor-pointer transition-colors hover:bg-elevated/40"
												>
													<td class="px-4 py-3 text-muted">{fmtDate(m.date)}</td>
													<td class="px-4 py-3">
														<StatusBadge label={meta.label} color={meta.color} />
													</td>
													<td
														class="px-4 py-3 text-right font-mono {m.type === 'in' ? 'text-success' : 'text-danger'}"
													>
														{m.type === 'in' ? '+' : '−'}{fmtQty(m.quantity, m.item.unit)}
													</td>
													<td class="px-4 py-3 text-muted">
														<a
															href={m.rotaTipo === 'entrada'
																? `/estoque/entradas/${m.rotaId}`
																: `/estoque/saidas/${m.rotaId}`}
															class="text-brandhi transition-colors hover:text-brand"
															onclick={(e) => e.stopPropagation()}
														>
															{m.reference ?? 'Ver detalhe'}
														</a>
													</td>
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
					</div>

				{:else if activeTab === 'emprestimos'}
					<div data-testid="it-tab-emprestimos">
						<EmptyState
							icon="arrow-uturn-left"
							title="Sem empréstimos"
							description="Nenhum empréstimo registrado para este item. (R-9: histórico por item pendente de contrato) 🟡"
						/>
					</div>

				{:else if activeTab === 'fornecedores'}
					<div data-testid="it-tab-fornecedores">
						<EmptyState
							icon="truck"
							title="Sem fornecedores"
							description="Nenhum fornecedor vinculado a este item."
						/>
					</div>

				{:else if activeTab === 'historico'}
					<div data-testid="it-tab-historico">
						<EmptyState
							icon="clock"
							title="Sem histórico"
							description="Nenhuma alteração registrada para este item."
						/>
					</div>
				{/if}
			</div>
		</section>
	</div>
{/if}

{#snippet specRow(label: string, value: string, extra = '')}
	<div class="contents">
		<dt class="bg-elevated/40 px-4 py-2.5 text-xs text-muted">{label}</dt>
		<dd class="px-4 py-2.5 text-sm {extra}">{value}</dd>
	</div>
{/snippet}